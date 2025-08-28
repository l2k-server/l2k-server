package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.skill.effect.event.DamageEvent
import org.l2kserver.game.model.utils.PHYSICAL_ATTACK_BASE
import org.l2kserver.game.model.utils.PHYSICAL_DMG_FROM_BACK_MODIFIER
import org.l2kserver.game.model.utils.PHYSICAL_DMG_FROM_SIDE_MODIFIER
import org.l2kserver.game.model.utils.calculateIsAvoided
import org.l2kserver.game.model.utils.calculateIsBlocked
import org.l2kserver.game.model.utils.calculateIsCritical
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Calculates hit dealt by `this` actor to [other]
 *
 * @param other Target of attack
 * @param attackPowerDivider Value, on which resulting damage should be divided
 * (for example dual weapon attack contains two hits, each should deal 50% damage)
 */
fun Actor.hit(other: Actor, attackPowerDivider: Int = 1): DamageEvent {
    val isAvoided = calculateIsAvoided(this, other)
    //TODO Calculate PerfectShieldBlock

    if (isAvoided) return DamageEvent(targetId = other.id, isAvoided = true)

    val isCritical = calculateIsCritical(this, other)
    val isBlocked = calculateIsBlocked(this, other)

    return DamageEvent(
        targetId = other.id,
        damage = calculateAutoAttackDamage(
            this, other, isCritical, isBlocked, usedSoulshot = false
        ) / attackPowerDivider,
        usedSoulshot = false, //TODO
        isCritical = isCritical,
        isBlocked = isBlocked,
    )
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
