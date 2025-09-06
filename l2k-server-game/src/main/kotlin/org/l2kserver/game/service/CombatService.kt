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
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.WeaponType
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

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /** Key - actor ID, value - time when actor can hit again */
    private val nextAttackAvailableTimeMap = ConcurrentHashMap<Int, Long>()

    /**
     * Start fighting with [attacked] - move enough close to hit and attack
     *
     * @param attacker Actor, who starts attacking
     * @param attacked Actor, who is the target of the attack
     */
    suspend fun launchAttack(attacker: MutableActorInstance, attacked: MutableActorInstance) {
        log.debug("Started attacking '{}' by '{}'", attacked, attacker)

        if (attacker.isParalyzed || attacker.isDead()) {
            log.debug("'{}' is paralyzed or dead and cannot attack", attacker)
            send(ActionFailedResponse)
            return
        }

        if (attacked.isDead()) {
            log.debug("'{}' is dead and cannot be attacked", attacked)
            send(SystemMessageResponse.IncorrectTarget)
            send(ActionFailedResponse)
            return
        }

        attacker.launchAction {
            while (isActive && !attacker.isParalyzed && !attacked.isDead() && gameObjectRepository.existsById(attacked.id)) {
                try {
                    val requiredDistance = (attacker.stats.attackRange + attacked.collisionBox.radius).roundToInt()

                    moveService.move(attacker, attacked, requiredDistance)

                    //Check if movement was interrupted or stopped at some obstacle
                    if (!attacker.position.isCloseTo(attacked.position, requiredDistance)) {
                        send(SystemMessageResponse.TargetOutOfRange)
                        break
                    }

                    // If next attack is not available, wait for a while and try again
                    if ((nextAttackAvailableTimeMap[attacker.id] ?: 0) > currentTimeMillis()) {
                        delay(50L)
                        continue
                    }

                    newSuspendedTransaction {
                        //Already launched attack must not be cancelled
                        withContext(coroutineContext + NonCancellable) {
                            when (attacker.weaponType) {
                                WeaponType.BOW -> performBowAttack(attacker, attacked)
                                WeaponType.POLE -> performPoleAttack(attacker, attacked)
                                WeaponType.FIST, WeaponType.DOUBLE_BLADES -> performSimpleAttacks(attacker, attacked, 2)
                                else -> performSimpleAttacks(attacker, attacked, 1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("An error occurred while attacking target {} by {}", attacked, attacker, e)
                    coroutineContext.cancel()
                }
            }
        }
    }

    suspend fun performDamage(damageEvent: DamageEffect, attacker: MutableActorInstance) {
        val attacked = gameObjectRepository.findActorById(damageEvent.targetId)
        if (attacked.isDead()) return //Needed for double weapon, if target was killed by first hit

        log.debug("{} has dealt {} damage to {}", attacker, damageEvent.damage, attacked)

        actorStateService.activateCombatState(attacker)
        actorStateService.activateCombatState(attacked)
        if (attacker is PlayerCharacter && attacked is PlayerCharacter && attacked.karma == 0) {
            actorStateService.activatePvpState(attacker)
        }

        if (damageEvent.isAvoided) {
            send(SystemMessageResponse.YouMissed)
            sendTo(
                attacked.id,
                SystemMessageResponse.YouHaveAvoidedAttackOf(attacker.name)
            )
            return
        }

        // Calculate overhit damage.
        // "mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40" (c) l2jserver
        val overhitDamage = if (damageEvent.overhitPossible && attacked is Npc)
            maxOf(damageEvent.damage - attacked.currentHp, 0)
        else 0

        //Store damage for AI and reward ownership
        if (attacked is Npc) synchronized(attacked.opponentsDamage) {
            val damageDealt = attacked.opponentsDamage[attacker] ?: 0
            attacked.opponentsDamage[attacker] = damageDealt + minOf(damageEvent.damage, attacked.currentHp)
        }

        //If fighters are players, subtract fom CP first
        if (attacker is PlayerCharacter && attacked is PlayerCharacter) {
            val hitOnHp = -minOf(attacked.currentCp - damageEvent.damage, 0)

            attacked.currentCp = maxOf(0, attacked.currentCp - damageEvent.damage)
            attacked.currentHp = maxOf(0, attacked.currentHp - hitOnHp)
        } else attacked.currentHp = maxOf(0, attacked.currentHp - damageEvent.damage)

        if (damageEvent.isCritical) send(SystemMessageResponse.CriticalHit)
        if (damageEvent.isBlocked) sendTo(attacked.id, SystemMessageResponse.ShieldDefenceSuccessful)

        send(SystemMessageResponse.YouHit(damageEvent.damage))
        sendTo(attacked.id, SystemMessageResponse.YouWereHitBy(attacker.name, damageEvent.damage))

        if (overhitDamage > 0) send(SystemMessageResponse.OverHit)

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
    private suspend fun performSimpleAttacks(attacker: MutableActorInstance, attacked: MutableActorInstance, hitAmount: Int) {
        log.debug("{} tries to perform {} attacks on {}", attacker, hitAmount, attacked)

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration

        val hits = List(hitAmount) { attacker.hit(attacked, attackPowerDivider = hitAmount) }

        val delayBeforeHit = attackDuration / (1 + hitAmount)
        broadcastPacket(AttackResponse(attacker, hits), attacker.position)

        //Delay for the time between start of the attack animation and the hit
        delay(delayBeforeHit)

        hits.forEach {
            performDamage(it, attacker)
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
                send(SystemMessageResponse.NotEnoughMp)
                coroutineContext.cancel()
                return
            }

            //If weapon consumes smth
            weapon.consumes?.let { consumable ->
                val arrows = attacker.inventory.findAllByTemplateId(consumable.id).firstOrNull()
                //Check if player has enough ammo
                if (arrows == null || consumable.amount > arrows.amount) {
                    send(SystemMessageResponse.NotEnoughArrows)
                    coroutineContext.cancel()
                    return
                }

                //Subtract ammo
                val updatedArrows = attacker.inventory.reduceAmount(arrows.id, consumable.amount)
                if (updatedArrows == null) send(UpdateItemsResponse.operationRemove(arrows))
                else send(UpdateItemsResponse.operationModify(updatedArrows))
            }

            //Subtract mana
            attacker.currentMp -= weapon.manaCost
            send(UpdateStatusResponse.hpMpCpOf(attacker))
        }

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        val reuseDelay = calculateBowAttackReuseTime(attacker.stats.atkSpd)

        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration + reuseDelay

        send(SystemMessageResponse.YouCarefullyNockAnArrow)
        val hit = attacker.hit(attacked)

        send(GaugeResponse(GaugeColor.RED, (attackDuration + reuseDelay).toInt()))
        broadcastPacket(AttackResponse(attacker, listOf(hit)), attacker.position)

        //Delay before launching an arrow
        delay((attackDuration * 0.9).roundToLong())

        //Launch an arrow!
        CoroutineScope(coroutineContext + NonCancellable).launch {
            //Delay for time it takes for the arrow to reach the target
            delay((attacker.position.distanceTo(attacked.position) / ARROW_SPEED_PER_MS).toLong())
            newSuspendedTransaction { performDamage(hit, attacker) }
        }

        //Delay for the time between the hit and the end of the attack animation
        delay((attackDuration * 0.1).roundToLong())
    }

    /**
     * Performs pole attack
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a target for this attack
     */
    private suspend fun performPoleAttack(attacker: MutableActorInstance, attacked: MutableActorInstance) {
        //TODO Pole attack
        log.debug("{} tries to hit {} by pole", attacker, attacked)
        send(SystemMessageResponse("Pole attack is not implemented yet"))
        send(ActionFailedResponse)
        coroutineContext.cancel()
    }

    private fun calculateAttackTime(atkSpd: Int) = DELAY_BETWEEN_ATTACKS_BASE / atkSpd

    private fun calculateBowAttackReuseTime(atkSpd: Int) = BOW_REUSE_DELAY / atkSpd

    /**
     * Kills actor, notifies surrounding players about it, performs required actions on actors death
     *
     * @param actor Actor, who was killed
     */
    private suspend fun killActor(actor: MutableActorInstance, killer: MutableActorInstance, overhitDamage: Int) {
        actor.cancelAction()
        actorStateService.disableCombatState(actor)

        when (actor) {
            is Npc -> {
                broadcastPacket(NpcDiedResponse(actor), actor.position)
                npcService.handleNpcDeath(actor)
                if (killer is PlayerCharacter) rewardService.manageRewardForKillingNpc(killer, actor, overhitDamage)
            }

            is PlayerCharacter -> {
                broadcastPacket(PlayerDiedResponse(actor), actor.position)
                if (killer is PlayerCharacter) rewardService.manageRewardForKillingPlayer(actor, killer)
            }
        }
    }

}
