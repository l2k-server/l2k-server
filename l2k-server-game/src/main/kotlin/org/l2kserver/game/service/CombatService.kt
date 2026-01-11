package org.l2kserver.game.service

import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.AttackResponse
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.NpcDiedResponse
import org.l2kserver.game.handler.dto.response.PlayerDiedResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.extensions.toInt
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val DELAY_BETWEEN_ATTACKS_BASE = 470_000L
private const val BOW_REUSE_DELAY = 499_500L
private const val ARROW_SPEED_PER_MS = 0.9

/** Service to handle fighting stuff - auto attacks, damage, etc. */
@Service
class CombatService(
    private val moveService: MoveService,
    private val actorStateService: ActorStateService,
    private val npcService: NpcService,
    private val rewardService: RewardService,
    private val asyncTaskService: AsyncTaskService,
    private val itemService: ItemService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Key - actor ID, value - time when actor can hit again */
    private val nextAttackAvailableTimeMap = ConcurrentHashMap<Int, Long>()


    /** Launches attacking job for player */
    suspend fun launchAttack(
        character: PlayerCharacterInstanceImpl, target: MutableActorInstance
    ) = asyncTaskService.launchAction(character.id) {
        attack(character, target)
    }

    /** Move [attacker] enough close to hit and attack [attacked] */
    suspend fun attack(attacker: MutableActorInstance, attacked: MutableActorInstance) {
        log.debug("Started attacking '{}' by '{}'", attacked, attacker)

        if (attacked.isDead()) {
            log.debug("'{}' is dead and cannot be attacked", attacked)
            send { SystemMessageResponse.IncorrectTarget }
            return
        }

        while (currentCoroutineContext().isActive && attacker.canAttack(attacked)) {
            try {
                val requiredDistance = (attacker.stats.attackRange + attacked.collisionBox.radius).roundToInt()

                moveService.move(attacker, attacked, requiredDistance)

                //Check if movement was interrupted or stopped at some obstacle
                if (!attacker.position.isCloseTo(attacked.position, requiredDistance)) {
                    send { SystemMessageResponse.TargetOutOfRange }
                    break
                }

                // If next attack is not available, wait for a while and try again
                if ((nextAttackAvailableTimeMap[attacker.id] ?: 0) > currentTimeMillis()) {
                    delay(50L)
                    continue
                }

                suspendTransaction {
                    // Player character must spend mana and arrows for attack (if weapon requires)
                    if ((attacker as? PlayerCharacterInstanceImpl)?.spendResources() != false)
                        //Already launched attack must not be cancelled TODO STUN??
                        withContext(currentCoroutineContext() + NonCancellable) {
                            when (attacker.weaponType) {
                                WeaponType.BOW ->
                                    performBowAttack(attacker, attacked)
                                WeaponType.FIST, WeaponType.DOUBLE_BLADES ->
                                    performSimpleAttacks(attacker, attacked, 2)
                                else ->
                                    performSimpleAttacks(attacker, attacked, 1)
                            }

                            // Activate combat stance and pvp state (if fighters are characters)
                            actorStateService.activateCombatState(attacker)
                            actorStateService.activateCombatState(attacked)
                            if (
                                attacker is PlayerCharacterInstance &&
                                attacked is PlayerCharacterInstance &&
                                attacked.karma == 0
                            ) {
                                actorStateService.activatePvpState(attacker)
                            }

                            //Enable SS if auto-use soulshot enabled
                            (attacker as? PlayerCharacterInstanceImpl)?.autoUsesSoulshot?.let {
                                itemService.useSoulshot(attacker, it)
                            }
                        }
                }
            } catch (e: Exception) {
                log.error("An error occurred while attacking target {} by {}", attacked, attacker, e)
                currentCoroutineContext().cancel()
            }
        }
    }

    suspend fun applyDamageEffect(
        attacker: MutableActorInstance, effect: DamageEffect, skill: ActiveSkillInstance? = null
    ) = suspendTransaction {
        val attacked = gameObjectRepository.findActorByIdOrNull(effect.targetId) ?: return@suspendTransaction

        //For double weapon, if target was killed by first hit, or if actor is already killed by smth else
        if (attacked.isDead()) return@suspendTransaction

        log.debug("{} has dealt {} damage to {}", attacker, effect.damage, attacked)

        if (effect.isAvoided) {
            sendTo(attacker.id) { SystemMessageResponse.YouMissed }
            sendTo(attacked.id) {
                SystemMessageResponse.YouHaveAvoidedAttackOf(attacker.name)
            }
            return@suspendTransaction
        }

        // Calculate overhit damage.
        // "mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40" (c) l2jserver
        val overhitDamage = if (skill?.overhitPossible == true && attacked is NpcInstanceImpl)
            maxOf(effect.damage - attacked.currentHp, 0)
        else 0

        //Store damage for AI and reward ownership
        if (attacked is NpcInstanceImpl) synchronized(attacked.opponents) {
            val damageDealt = attacked.opponents[attacker] ?: 0
            attacked.opponents[attacker] = damageDealt + minOf(effect.damage, attacked.currentHp)
        }

        //If fighters are players, subtract fom CP first
        val damageOnHp = if (attacker is PlayerCharacterInstanceImpl && attacked is PlayerCharacterInstanceImpl) {
            val hitOnHp = -minOf(attacked.currentCp - effect.damage, 0)
            attacked.currentCp = maxOf(0, attacked.currentCp - effect.damage)
            hitOnHp
        } else effect.damage

        val minHpAfterHit = (!effect.isDeathly).toInt()
        attacked.currentHp = maxOf(minHpAfterHit, attacked.currentHp - damageOnHp)

        if (effect.isCritical)
            sendTo(attacker.id) { SystemMessageResponse.CriticalHit }
        if (effect.isMagicCritical)
            sendTo(attacker.id) { SystemMessageResponse.MagicCriticalHit }
        if (effect.isHalfSuccessful)
            sendTo(attacker.id) { SystemMessageResponse.AttackFailed }
        if (skill != null && effect.isFailed)
            sendTo(attacker.id) { SystemMessageResponse.HasResisted(attacked.name, skill) }
        if (effect.isBlocked)
            sendTo(attacked.id) { SystemMessageResponse.ShieldDefenceSuccessful }

        sendTo(attacker.id) { SystemMessageResponse.YouHit(effect.damage) }
        sendTo(attacked.id) {
            SystemMessageResponse.YouWereHitBy(attacker.name, effect.damage)
        }

        if (overhitDamage > 0) send { SystemMessageResponse.OverHit }

        val updatedStatus = UpdateStatusResponse.hpMpCpOf(attacked)
        sendTo(attacked.id) { updatedStatus }
        attacked.targetedBy.forEach { sendTo(it.id) { updatedStatus }}

        if (attacked.currentHp == 0) killActor(attacked, attacker, overhitDamage)
    }

    /**
     * Performs [hitAmount] melee attacks.
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a target for this attack
     * @param hitAmount How many hits does the attack contain
     */
    private suspend fun performSimpleAttacks(
        attacker: MutableActorInstance, attacked: MutableActorInstance, hitAmount: Int
    ) {
        log.debug("{} tries to perform {} attacks on {}", attacker, hitAmount, attacked)

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration

        val weapon = (attacker as? PlayerCharacterInstanceImpl)?.inventory?.weapon
        val soulshotUsed = weapon?.soulshotCharged ?: false

        val aoeTargets = getAoeAttackTargets(attacker, attacked)

        val hits = List(hitAmount) {
            val effects = aoeTargets.map {
                DamageEffect.physicalHit(
                    attacker, it, usedSoulshot = soulshotUsed, attackPowerDivider = hitAmount
                )
            }.toMutableList()

            effects.add(DamageEffect.physicalHit(
                attacker, attacked, usedSoulshot = soulshotUsed, attackPowerDivider = hitAmount
            ))

            effects
        }

        if (soulshotUsed && hits.flatten().any { !it.isAvoided }) attacker.inventory.weapon?.soulshotCharged = false

        val delayBeforeHit = attackDuration / (1 + hitAmount)

        broadcastAround(attacker.position) {
            AttackResponse(attacker, hits.flatten(), soulshotUsed)
        }

        //Delay for the time between start of the attack animation and the hit
        delay(delayBeforeHit)

        hits.forEach { hitEffects ->
            hitEffects.forEach { applyDamageEffect(attacker, it) }
            delay(delayBeforeHit)
        }
    }

    /**
     * Performs bow attack
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a target for this attack
     */
    private suspend fun performBowAttack(attacker: MutableActorInstance, attacked: MutableActorInstance) {
        log.debug("{} tries to hit {} by bow", attacker, attacked)

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        val reuseDelay = calculateBowAttackReuseTime(attacker.stats.atkSpd)

        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration + reuseDelay

        send { SystemMessageResponse.YouCarefullyNockAnArrow }

        val usedSoulshot = (attacker as? PlayerCharacterInstanceImpl)?.inventory?.weapon?.soulshotCharged ?: false

        val damageEffect = DamageEffect.physicalHit(attacker, attacked, usedSoulshot = usedSoulshot)
        if (usedSoulshot && !damageEffect.isAvoided) attacker.inventory.weapon?.soulshotCharged = false

        send { GaugeResponse(GaugeColor.RED, (attackDuration + reuseDelay).toInt()) }
        broadcastAround(attacker.position) {
            AttackResponse(attacker, damageEffect, usedSoulshot)
        }

        //Delay before launching an arrow
        delay((attackDuration * 0.9).roundToLong())

        //Launch an arrow!
        CoroutineScope(currentCoroutineContext() + NonCancellable).launch {
            //Delay for time it takes for the arrow to reach the target
            delay((attacker.position.distanceTo(attacked.position) / ARROW_SPEED_PER_MS).toLong())
            suspendTransaction { applyDamageEffect(attacker, damageEffect) }
        }

        //Delay for the time between the hit and the end of the attack animation
        delay((attackDuration * 0.1).roundToLong())
    }

    /**
     * Finds targets for AoE attacks
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a main target for this attack
     */
    private suspend fun getAoeAttackTargets(
        attacker: MutableActorInstance, attacked: MutableActorInstance
    ): Sequence<MutableActorInstance> {
        val sweepRange = (attacker.stats.attackRange * 1.5).roundToInt()

        val headingToMainTarget = attacker.position.headingTo(attacked.position)
        val minHeading = (headingToMainTarget - attacker.stats.aoeHitSpread).value
        val maxHeading = (headingToMainTarget + attacker.stats.aoeHitSpread).value

        return gameObjectRepository
            .findAllActorsNear(attacker, sweepRange)
            .filter { it != attacked && it != attacker }
            .filter { it.isEnemyOf(attacker) && !it.isDead() }
            .filter {
                val headingToIt = attacker.position.headingTo(it.position).value

                if (minHeading < maxHeading) headingToIt in minHeading..maxHeading
                else {
                    headingToIt in minHeading..UShort.MAX_VALUE || headingToIt in UShort.MIN_VALUE..maxHeading
                }
            }
            .shuffled()
            .take(attacker.stats.aoeTargetsAmount)
    }

    private fun calculateAttackTime(atkSpd: Int) = DELAY_BETWEEN_ATTACKS_BASE / atkSpd

    private fun calculateBowAttackReuseTime(atkSpd: Int) = BOW_REUSE_DELAY / atkSpd

    /**
     * Kills actor, notifies surrounding players about it, performs required actions on actors death
     *
     * @param actor Actor, who was killed
     */
    private suspend fun killActor(actor: MutableActorInstance, killer: MutableActorInstance, overhitDamage: Int) {
        asyncTaskService.cancelActionByActorId(actor.id)
        actorStateService.disableCombatState(actor)

        //TODO Noblesse Blessing
        actor.temporalEffects.clear()

        when (actor) {
            is NpcInstanceImpl -> {
                broadcastAround(actor.position) { NpcDiedResponse(actor) }
                npcService.handleNpcDeath(actor)
                if (killer is PlayerCharacterInstanceImpl)
                    rewardService.manageRewardForKillingNpc(killer, actor, overhitDamage)
            }

            is PlayerCharacterInstanceImpl -> {
                broadcastAround(actor.position) { PlayerDiedResponse(actor) }
                if (killer is PlayerCharacterInstanceImpl) rewardService.manageRewardForKillingPlayer(actor, killer)
            }
        }
    }

    /** Checks if `this` can attack [target] and sends system messages */
    private suspend fun ActorInstance.canAttack(target: ActorInstance) = when {
        this.isParalyzed || this.isDead() -> {
            send { ActionFailedResponse }
            false
        }
        target.isDead() -> {
            send { ActionFailedResponse }
            false
        }

        !target.exists() || !this.position.isCloseTo(target.position, VISION_RANGE) -> {
            send { ActionFailedResponse }
            false
        }

        else -> true
    }

    /**
     * Spends resources for attack
     *
     * @return `true` if resources are spent and attack can be performed, `false` - if not
     */
    private suspend fun PlayerCharacterInstanceImpl.spendResources(): Boolean {
        val weapon = this.inventory.weapon ?: return true

        //Check if player has enough mana
        if (this.currentMp < weapon.manaCost) {
            send { SystemMessageResponse.NotEnoughMp }
            currentCoroutineContext().cancel() //TODO cancelling whole process seems to be not very good idea...
            return false
        }

        //If weapon consumes smth
        weapon.consumes?.let { consumable ->
            val arrows = this.inventory.findAllByTemplateId(consumable.templateId).firstOrNull()
            //Check if player has enough ammo
            if (arrows == null || consumable.amount > arrows.amount) {
                send { SystemMessageResponse.NotEnoughArrows }
                currentCoroutineContext().cancel()
                return false
            }

            //Subtract ammo
            val updatedArrows = this.inventory.reduceAmount(arrows.id, consumable.amount)
            if (updatedArrows == null) send { UpdateItemsResponse().wasDeleted(arrows) }
            else send { UpdateItemsResponse().wasModified(updatedArrows) }
        }

        if (weapon.manaCost != 0) {
            //Subtract mana
            this.currentMp -= weapon.manaCost
            send { UpdateStatusResponse.hpMpCpOf(this) }
        }

        return true
    }

}
