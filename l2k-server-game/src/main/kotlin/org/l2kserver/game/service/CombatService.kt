package org.l2kserver.game.service

import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.hit
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.AttackResponse
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.NpcDiedResponse
import org.l2kserver.game.handler.dto.response.PlayerDiedResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.handler.dto.response.toAttack
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.model.skill.action.effect.DamageEffect
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val DELAY_BETWEEN_ATTACKS_BASE = 470_000L
private const val BOW_REUSE_DELAY = 499_500L
private const val ARROW_SPEED_PER_MS = 0.9

/**
 * Service to handle all fighting stuff - auto attacks, offensive skills, damage, etc.
 */
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
        character: PlayerCharacter, target: MutableActorInstance
    ) = asyncTaskService.launchAction(character.id) {
        attack(character, target)
    }

    /** Move [attacker] enough close to hit and attack [attacked] */
    suspend fun attack(attacker: MutableActorInstance, attacked: MutableActorInstance) {
        log.debug("Started attacking '{}' by '{}'", attacked, attacker)
        while (coroutineContext.isActive && attacker.canAttack(attacked)) {
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

                newSuspendedTransaction {
                    //Already launched attack must not be cancelled TODO STUN??
                    withContext(coroutineContext + NonCancellable) {
                        when (attacker.weaponType) {
                            WeaponType.BOW ->
                                performBowAttack(attacker, attacked)
                            WeaponType.FIST, WeaponType.DOUBLE_BLADES ->
                                performSimpleAttacks(attacker, attacked, 2)
                            else ->
                                performSimpleAttacks(attacker, attacked, 1)
                        }

                        //Enable SS if auto-use soulshot enabled
                        (attacker as? PlayerCharacter)?.autoUsesSoulshot?.let {
                            itemService.useSoulshot(attacker, it)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("An error occurred while attacking target {} by {}", attacked, attacker, e)
                coroutineContext.cancel()
            }
        }
    }

    suspend fun applyDamageEffect(
        attacker: MutableActorInstance,
        attacked: MutableActorInstance,
        damageEffect: DamageEffect,
        skill: Skill? = null,
        overhitPossible: Boolean = false
    ) {
        //For double weapon, if target was killed by first hit, or if actor is already killed by smth else
        if (attacked.isDead()) return

        log.debug("{} has dealt {} damage to {}", attacker, damageEffect.damage, attacked)

        actorStateService.activateCombatState(attacker)
        actorStateService.activateCombatState(attacked)
        if (attacker is PlayerCharacter && attacked is PlayerCharacter && attacked.karma == 0) {
            actorStateService.activatePvpState(attacker)
        }

        if (damageEffect.isAvoided) {
            send { SystemMessageResponse.YouMissed }
            sendTo(
                attacked.id,
                SystemMessageResponse.YouHaveAvoidedAttackOf(attacker.name)
            )
            return
        }

        // Calculate overhit damage.
        // "mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40" (c) l2jserver
        val overhitDamage = if (overhitPossible && attacked is Npc)
            maxOf(damageEffect.damage - attacked.currentHp, 0)
        else 0

        //Store damage for AI and reward ownership
        if (attacked is Npc) synchronized(attacked.opponents) {
            val damageDealt = attacked.opponents[attacker] ?: 0
            attacked.opponents[attacker] = damageDealt + minOf(damageEffect.damage, attacked.currentHp)
        }

        //If fighters are players, subtract fom CP first
        if (attacker is PlayerCharacter && attacked is PlayerCharacter) {
            val hitOnHp = -minOf(attacked.currentCp - damageEffect.damage, 0)

            attacked.currentCp = maxOf(0, attacked.currentCp - damageEffect.damage)
            attacked.currentHp = maxOf(0, attacked.currentHp - hitOnHp)
        } else attacked.currentHp = maxOf(0, attacked.currentHp - damageEffect.damage)

        if (damageEffect.isCritical)
            send { SystemMessageResponse.CriticalHit }
        if (damageEffect.isMagicCritical)
            send { SystemMessageResponse.MagicCriticalHit }
        if (damageEffect.isHalfSuccessful)
            send { SystemMessageResponse.AttackFailed }
        if (skill != null && damageEffect.isFailed)
            send { SystemMessageResponse.HasResisted(attacked.name, skill) }
        if (damageEffect.isBlocked)
            sendTo(attacked.id, SystemMessageResponse.ShieldDefenceSuccessful)

        send { SystemMessageResponse.YouHit(damageEffect.damage) }
        sendTo(attacked.id, SystemMessageResponse.YouWereHitBy(attacker.name, damageEffect.damage))

        if (overhitDamage > 0) send { SystemMessageResponse.OverHit }

        val updatedStatus = UpdateStatusResponse.hpMpCpOf(attacked)
        sendTo(attacked.id, updatedStatus)
        attacked.targetedBy.forEach { sendTo(it.id, updatedStatus) }

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

        val weapon = (attacker as? PlayerCharacter)?.inventory?.weapon
        val soulshotUsed = weapon?.soulshotCharged ?: false

        val effectLists = mutableListOf(
            attacked to List(hitAmount) {
                attacker.hit(attacked, soulshotUsed, hitAmount)
            }
        )

        if (attacker.stats.aoeTargetsAmount > 0) effectLists += getAoeAttackTargets(attacker, attacked).map { aoeTarget ->
            aoeTarget to List(hitAmount) {
                attacker.hit(attacked, soulshotUsed, hitAmount)
            }
        }

        if (soulshotUsed && effectLists.map { it.second }.flatten().any { !it.isAvoided })
            attacker.inventory.weapon?.soulshotCharged = false

        val delayBeforeHit = attackDuration / (1 + hitAmount)

        val attacks = effectLists.map { (target, effects) -> effects.map { it.toAttack(target.id) }}.flatten()
        broadcastAround(attacker.position) { AttackResponse(attacker, attacks, soulshotUsed) }

        //Delay for the time between start of the attack animation and the hit
        delay(delayBeforeHit)


        repeat(hitAmount) { hitNumber ->
            effectLists.forEach { (target, effect) ->
                applyDamageEffect(attacker, target, effect[hitNumber])
            }

            //Delay for the time between the hit and the end of the attack animation
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

        // Consume mana and arrows for shot, if attacker is PlayerCharacter
        if (attacker is PlayerCharacter) {
            val weapon = attacker.inventory.weapon!!

            //Check if player has enough mana
            if (attacker.currentMp < weapon.manaCost) {
                send { SystemMessageResponse.NotEnoughMp }
                coroutineContext.cancel() //TODO cancelling whole process seems to be not very good idea...
                return
            }

            //If weapon consumes smth
            weapon.consumes?.let { consumable ->
                val arrows = attacker.inventory.findAllByTemplateId(consumable.id).firstOrNull()
                //Check if player has enough ammo
                if (arrows == null || consumable.amount > arrows.amount) {
                    send { SystemMessageResponse.NotEnoughArrows }
                    coroutineContext.cancel()
                    return
                }

                //Subtract ammo
                val updatedArrows = attacker.inventory.reduceAmount(arrows.id, consumable.amount)
                if (updatedArrows == null) send { UpdateItemsResponse().wasDeleted(arrows) }
                else send { UpdateItemsResponse().wasModified(updatedArrows) }
            }

            //Subtract mana
            attacker.currentMp -= weapon.manaCost
            send { UpdateStatusResponse.hpMpCpOf(attacker) }
        }

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        val reuseDelay = calculateBowAttackReuseTime(attacker.stats.atkSpd)

        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration + reuseDelay

        send { SystemMessageResponse.YouCarefullyNockAnArrow }

        val usedSoulshot = (attacker as? PlayerCharacter)?.inventory?.weapon?.soulshotCharged ?: false

        val damageEffect = attacker.hit(attacked, usedSoulshot)
        if (usedSoulshot && !damageEffect.isAvoided) attacker.inventory.weapon?.soulshotCharged = false

        send { GaugeResponse(GaugeColor.RED, (attackDuration + reuseDelay).toInt()) }
        broadcastAround(attacker.position) {
            AttackResponse(attacker, damageEffect.toAttack(attacked.id), usedSoulshot)
        }

        //Delay before launching an arrow
        delay((attackDuration * 0.9).roundToLong())

        //Launch an arrow!
        CoroutineScope(coroutineContext + NonCancellable).launch {
            //Delay for time it takes for the arrow to reach the target
            delay((attacker.position.distanceTo(attacked.position) / ARROW_SPEED_PER_MS).toLong())
            newSuspendedTransaction { applyDamageEffect(attacker, attacked, damageEffect) }
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

        when (actor) {
            is Npc -> {
                broadcastAround(actor.position) { NpcDiedResponse(actor) }
                npcService.handleNpcDeath(actor)
                if (killer is PlayerCharacter) rewardService.manageRewardForKillingNpc(killer, actor, overhitDamage)
            }

            is PlayerCharacter -> {
                broadcastAround(actor.position) { PlayerDiedResponse(actor) }
                if (killer is PlayerCharacter) rewardService.manageRewardForKillingPlayer(actor, killer)
            }
        }
    }

    /** Checks if `this` can attack [target] and sends system messages */
    private suspend fun ActorInstance.canAttack(target: ActorInstance) = when {
        this.isParalyzed || this.isDead() -> {
            log.debug("'{}' is paralyzed or dead and cannot attack '{}'", this, target)
            send { ActionFailedResponse }
            false
        }

        target.isDead() -> {
            log.debug("'{}' is dead and cannot be attacked", target)
            send { SystemMessageResponse.IncorrectTarget }
            send { ActionFailedResponse }
            false
        }

        !gameObjectRepository.existsById(target.id)
                || !this.position.isCloseTo(target.position, VISION_RANGE) -> {
            false
        }

        else -> true
    }

}
