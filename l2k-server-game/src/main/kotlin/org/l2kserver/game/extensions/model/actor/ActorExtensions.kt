package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.calculateRandomDamageModifier
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.instance.CastableSkillInstance
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.PHYSICAL_DMG_FROM_BACK_MODIFIER
import org.l2kserver.game.model.utils.PHYSICAL_DMG_FROM_SIDE_MODIFIER
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackAvoided
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackBlocked
import org.l2kserver.game.model.utils.calculateIsPhysicalAttackCritical
import kotlin.math.roundToInt

/**
 * Calculates hit dealt by `this` actor to [other]
 *
 * @param other Target of attack
 * @param attackPowerDivider Value, on which resulting damage should be divided
 * (for example dual weapon attack contains two hits, each should deal 50% damage)
 */
fun ActorInstance.hit(other: ActorInstance, soulshotUsed: Boolean, attackPowerDivider: Int = 1): DamageEffect {
    val isAvoided = calculateIsPhysicalAttackAvoided(this, other)
    //TODO Calculate PerfectShieldBlock

    if (isAvoided) return DamageEffect(targetId = other.id, isAvoided = true)

    val isCritical = calculateIsPhysicalAttackCritical(this, other)
    val isBlocked = calculateIsPhysicalAttackBlocked(this, other)

    return DamageEffect(
        targetId = other.id,
        damage = calculateAutoAttackDamage(
            this, other, isCritical, isBlocked, soulshotUsed
        ) / attackPowerDivider,
        isCritical = isCritical,
        isBlocked = isBlocked,
    )
}

private fun calculateAutoAttackDamage(
    attacker: ActorInstance, attacked: ActorInstance, isCritical: Boolean, isBlocked: Boolean, usedSoulshot: Boolean
): Int {
    var damage = attacker.stats.pAtk.toDouble()
    if (usedSoulshot) damage *= 2
    if (isCritical) damage = damage * 2 /*TODO * Buffs multipliers*/ + attacker.stats.critDamage

    var defence = attacked.stats.pDef
    if (isBlocked) defence += attacked.stats.shieldDef

    //TODO calculate vulnerabilities and resistances
    damage = (PHYSICAL_ATTACK_BASE * damage) / defence

    if (attacker.isOnSideOf(attacked)) damage *= PHYSICAL_DMG_FROM_SIDE_MODIFIER
    if (attacker.isBehind(attacked)) damage *= PHYSICAL_DMG_FROM_BACK_MODIFIER

    //TODO calculate PvP bonus
    //TODO calculate PvP penalty

    damage *= attacker.weaponType.calculateRandomDamageModifier()

    return damage.roundToInt()
}

fun ActorInstance.asMutable() = requireNotNull(this as? MutableActorInstance) {
    "$this cannot be mutable"
}

/** Checks if actor has enough HP to cast [skill] */
fun ActorInstance.hasEnoughHpToCast(skill: CastableSkillInstance) =
    (skill.consumes?.hp ?: 0) + (skill.consumesToStart?.hp ?: 0) <= this.currentHp

/** Checks if actor has enough MP to cast [skill] */
fun ActorInstance.hasEnoughMpToCast(skill: CastableSkillInstance) =
    (skill.consumes?.mp ?: 0) + (skill.consumesToStart?.mp ?: 0) <= this.currentMp

/** Checks if PlayerCharacter has enough consumable item in the inventory to cast [skill]*/
fun PlayerCharacter.hasEnoughConsumableItemFor(skill: CastableSkillInstance): Boolean {
    val consumableToStart = skill.consumesToStart?.item
    val consumable = skill.consumes?.item

    return when {
        consumableToStart == null && consumable == null -> true

        consumableToStart?.id == consumable?.id -> this.inventory.existsByIdAndAmount(
            consumable!!.id, consumable.amount + consumableToStart!!.amount
        )

        else -> {
            consumableToStart?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
                    && consumable?.let { this.inventory.existsByIdAndAmount(it.id, it.amount) } != false
        }
    }
}
