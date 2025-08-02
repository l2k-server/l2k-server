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
import org.l2kserver.game.extensions.model.item.findAllByOwnerIdAndTemplateId
import org.l2kserver.game.extensions.model.item.reduceAmountBy
import org.l2kserver.game.handler.dto.response.ActionFailedResponse
import org.l2kserver.game.handler.dto.response.AttackResponse
import org.l2kserver.game.handler.dto.response.Hit
import org.l2kserver.game.handler.dto.response.GaugeColor
import org.l2kserver.game.handler.dto.response.GaugeResponse
import org.l2kserver.game.handler.dto.response.NpcDiedResponse
import org.l2kserver.game.handler.dto.response.PlayerDiedResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.handler.dto.response.UpdateItemsResponse
import org.l2kserver.game.handler.dto.response.UpdateStatusResponse
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

private const val DELAY_BETWEEN_ATTACKS_BASE = 470_000L
private const val BOW_REUSE_DELAY = 499_500L
private const val ARROW_SPEED_PER_MS = 0.9

private const val ACCURACY_FROM_SIDE_MODIFIER = 1.1
private const val ACCURACY_FROM_BACK_MODIFIER = 1.3

private const val CRIT_RATE_FROM_SIDE_MODIFIER = 1.1
private const val CRIT_RATE_FROM_BACK_MODIFIER = 1.2

private const val PHYSICAL_ATTACK_BASE = 70
private const val PHYSICAL_DMG_FROM_SIDE_MODIFIER = 1.1
private const val PHYSICAL_DMG_FROM_BACK_MODIFIER = 1.2

private const val EVASION_CHANCE_BASE = 88

private const val BONUS_SHIELD_DEF_RATE_AGAINST_BOW = 30
private const val BONUS_SHIELD_DEF_RATE_AGAINST_DAGGER = 12

/**
 * Service to handle all fighting stuff - auto attacks, offensive skills, damage, etc.
 */
@Service
class CombatService(
    private val asyncTaskService: AsyncTaskService,
    private val moveService: MoveService,
    private val actorStateService: ActorStateService,
    private val npcService: NpcService,
    private val rewardService: RewardService,

    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    /**
     * Key - actor ID, value - time when actor can hit again
     */
    private val nextAttackAvailableTimeMap = ConcurrentHashMap<Int, Long>()

    /**
     * Start fighting with [attacked] - move enough close to hit and attack
     *
     * @param attacker Actor, who starts attacking
     * @param attacked Actor, who is the target of the attack
     */
    suspend fun launchAttack(attacker: Actor, attacked: Actor) {
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

        asyncTaskService.launchAction(attacker.id) {
            while (isActive && !attacked.isDead() && gameObjectRepository.existsById(attacked.id)) {
                try {
                    moveService.move(attacker, attacked, attacker.stats.attackRange)

                    if (!attacker.isEnoughCloseToAttack(attacked)) {
                        send(SystemMessageResponse.TargetOutOfRange)
                        break
                    }

                    // If next attack is not available, wait for a while and try again
                    if ((nextAttackAvailableTimeMap[attacker.id] ?: 0) > currentTimeMillis()) {
                        delay(50L)
                        continue
                    }

                    actorStateService.activateCombatState(attacker)
                    if (attacker is PlayerCharacter && attacked is PlayerCharacter && attacked.karma == 0) {
                        actorStateService.activatePvpState(attacker)
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

    /**
     * Performs [hitAmount] melee attacks.
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a target for this attack
     * @param hitAmount How many hits does the attack contain
     */
    private suspend fun performSimpleAttacks(attacker: Actor, attacked: Actor, hitAmount: Int) {
        log.debug("{} tries to perform {} attacks on {}", attacker, hitAmount, attacked)

        val attackDuration = calculateAttackTime(attacker.stats.atkSpd)
        nextAttackAvailableTimeMap[attacker.id] = currentTimeMillis() + attackDuration

        val hits = List(hitAmount) { calculateAttack(attacker, attacked, hitAmount) }

        val delayBeforeHit = attackDuration / (1 + hitAmount)
        broadcastPacket(AttackResponse(attacker, hits), attacker.position)

        //Delay for the time between start of the attack animation and the hit
        delay(delayBeforeHit)

        hits.forEach {
            performHit(it, attacked, attacker)
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
    private suspend fun performBowAttack(attacker: Actor, attacked: Actor) {
        log.debug("{} tries to hit {} by bow", attacker, attacked)

        // Consume mana and arrows for shot, if attacker is PlayerCharacter
        if (attacker is PlayerCharacter) {
            val weapon = attacker.paperDoll.getWeapon()!!

            //Check if player has enough mana
            if (attacker.currentMp < weapon.manaCost) {
                send(SystemMessageResponse.NotEnoughMP)
                coroutineContext.cancel()
                return
            }

            //If weapon consumes smth
            weapon.consumes?.let { consumable ->
                val arrows = Item.findAllByOwnerIdAndTemplateId(attacker.id, consumable.id).firstOrNull()
                //Check if player has enough ammo
                if (arrows == null || consumable.amount > arrows.amount) {
                    send(SystemMessageResponse.NotEnoughArrows)
                    coroutineContext.cancel()
                    return
                }

                //Subtract ammo
                val updatedArrows = arrows.reduceAmountBy(consumable.amount)
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
        val attack = calculateAttack(attacker, attacked, 1)

        send(GaugeResponse(GaugeColor.RED, (attackDuration + reuseDelay).toInt()))
        broadcastPacket(AttackResponse(attacker, listOf(attack)), attacker.position)

        //Delay before launching an arrow
        delay((attackDuration * 0.9).roundToLong())

        //Launch an arrow!
        CoroutineScope(coroutineContext + NonCancellable).launch {
            //Delay for time it takes for the arrow to reach the target
            delay((attacker.position.distanceTo(attacked.position) / ARROW_SPEED_PER_MS).toLong())
            newSuspendedTransaction { performHit(attack, attacked, attacker) }
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
    private suspend fun performPoleAttack(attacker: Actor, attacked: Actor) {
        //TODO Pole attack
        log.debug("{} tries to hit {} by pole", attacker, attacked)
        send(SystemMessageResponse("Pole attack is not implemented yet"))
        send(ActionFailedResponse)
        coroutineContext.cancel()
    }

    private suspend fun performHit(hit: Hit, attacked: Actor, attacker: Actor) {
        if (attacked.isDead()) return //Needed for double weapon, if target was killed by first hit

        actorStateService.activateCombatState(attacked)

        if (hit.isAvoided) {
            send(SystemMessageResponse.YouMissed)
            sendTo(attacked.id, SystemMessageResponse.YouHaveAvoidedAttackOf(attacker.name))
            return
        }

        //If fighters are players, subtract fom CP first
        if (attacker is PlayerCharacter && attacked is PlayerCharacter) {
            val hitOnHp = -minOf(attacked.currentCp - hit.damage, 0)

            attacked.currentCp = maxOf(0, attacked.currentCp - hit.damage)
            attacked.currentHp = maxOf(0, attacked.currentHp - hitOnHp)
        }
        else attacked.currentHp = maxOf(0, attacked.currentHp - hit.damage)

        if (attacked is Npc) synchronized(attacked.opponentsDamage) {
            val damageDealt = attacked.opponentsDamage[attacker] ?: 0
            attacked.opponentsDamage[attacker] = damageDealt + hit.damage
        }

        if (hit.isCritical) send(SystemMessageResponse.CriticalHit)
        if (hit.isBlocked) sendTo(attacked.id, SystemMessageResponse.ShieldDefenceSuccessful)

        send(SystemMessageResponse.YouHit(hit.damage))
        sendTo(attacked.id, SystemMessageResponse.YouWereHitBy(attacker.name, hit.damage))

        val updatedStatus = UpdateStatusResponse.hpMpCpOf(attacked)
        sendTo(attacked.id, updatedStatus)
        attacked.targetedBy.forEach { sendTo(it, updatedStatus) }

        if (attacked.currentHp == 0) killActor(attacked, attacker)
    }

    /**
     * Calculates Attack - is it successful, critical, blocked, and it's damage
     *
     * @param attacker Actor,who performs the attack
     * @param attacked Actor, who is a target for this attack
     * @param attackPowerDivider Value, on which resulting damage should be divided
     * (for example dual weapon hit contains two attacks, each should deal 50% damage)
     *
     * @return attack data
     */
    private fun calculateAttack(attacker: Actor, attacked: Actor, attackPowerDivider: Int): Hit {
        val isAvoided = calculateIsAvoided(attacker, attacked)
        //TODO Calculate PerfectShieldBlock

        return if (isAvoided) Hit(targetId = attacked.id, isAvoided = true) else {
            val isCritical = calculateIsCritical(attacker, attacked)
            val isBlocked = calculateIsBlocked(attacker.weaponType, attacked)

            Hit(
                targetId = attacked.id,
                damage = calculateAutoAttackDamage(
                    attacker, attacked, isCritical, isBlocked, false //TODO Manage using soulshots
                ) / attackPowerDivider,
                usedSoulshot = false, //TODO
                isCritical = isCritical,
                isBlocked = isBlocked,
            )
        }
    }

    private fun calculateAutoAttackDamage(
        attacker: Actor, attacked: Actor, isCritical: Boolean, isBlocked: Boolean, usedSoulshot: Boolean
    ): Int {
        var damage = attacker.stats.pAtk
        if (usedSoulshot) damage *= 2
        if (isCritical) damage = damage * 2 /*TODO * Buffs multipliers*/ + attacker.stats.critDamage

        var defence = attacked.stats.pDef
        if (isBlocked) defence += attacked.stats.shieldDef

        //TODO calculate vulnerabilities and resistances
        damage = (PHYSICAL_ATTACK_BASE * damage) / defence

        if (attacker.isOnSideOf(attacked))
            damage = (damage * PHYSICAL_DMG_FROM_SIDE_MODIFIER).roundToInt()
        if (attacker.isBehind(attacked))
            damage = (damage * PHYSICAL_DMG_FROM_BACK_MODIFIER).roundToInt()

        //TODO calculate PvP bonus
        //TODO calculate PvP penalty

        val randomModifier = attacker.weaponType?.randomCoefficient?.let { Random.nextInt(-it, it) } ?: 0
        damage += (randomModifier * damage) / 100

        return damage
    }

    private fun calculateIsCritical(attacker: Actor, attacked: Actor): Boolean {
        var critRate = attacker.stats.critRate

        if (attacker.isOnSideOf(attacked))
            critRate = (critRate * CRIT_RATE_FROM_SIDE_MODIFIER).roundToInt()
        if (attacker.isBehind(attacked))
            critRate = (critRate * CRIT_RATE_FROM_BACK_MODIFIER).roundToInt()

        return critRate > Random.nextInt(0, 1000)
    }

    private fun calculateIsAvoided(attacker: Actor, attacked: Actor): Boolean {
        var hitChance = EVASION_CHANCE_BASE + 2 * (attacker.stats.accuracy - attacked.stats.evasion)

        if (attacker.isOnSideOf(attacked))
            hitChance = (hitChance * ACCURACY_FROM_SIDE_MODIFIER).roundToInt()
        if (attacker.isBehind(attacked))
            hitChance = (hitChance * ACCURACY_FROM_BACK_MODIFIER).roundToInt()

        return hitChance < Random.nextInt(0, 100)
    }

    private fun calculateIsBlocked(attackerWeaponType: WeaponType?, attacked: Actor): Boolean {
        val attackerWeaponBonus = if (!attacked.hasShield) 0 else when (attackerWeaponType) {
            WeaponType.BOW -> BONUS_SHIELD_DEF_RATE_AGAINST_BOW
            WeaponType.DAGGER -> BONUS_SHIELD_DEF_RATE_AGAINST_DAGGER
            else -> 0
        }

        val blockChance = attacked.stats.shieldDefRate /* TODO * buff shield rate */ + attackerWeaponBonus
        return blockChance > Random.nextInt(0, 100)
    }

    private fun calculateAttackTime(atkSpd: Int) = DELAY_BETWEEN_ATTACKS_BASE / atkSpd

    private fun calculateBowAttackReuseTime(atkSpd: Int) = BOW_REUSE_DELAY / atkSpd

    /**
     * Kills actor, notifies surrounding players about it, performs required actions on actors death
     *
     * @param actor Actor, who was killed
     */
    private suspend fun killActor(actor: Actor, killer: Actor) {
        asyncTaskService.cancelActionByActorId(actor.id)
        actorStateService.disableCombatState(actor)

        when (actor) {
            is Npc -> {
                broadcastPacket(NpcDiedResponse(actor), actor.position)
                npcService.handleNpcDeath(actor)
                rewardService.manageRewardForKillingNpc(actor)
            }
            is PlayerCharacter -> {
                broadcastPacket(PlayerDiedResponse(actor), actor.position)
                rewardService.manageRewardForKillingPlayer(actor, killer)
            }
        }
    }

}
