package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.Hit
import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.item.template.WeaponType
import kotlin.math.roundToInt
import kotlin.random.Random

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
 * Calculates hit dealt by `this` actor to [other]
 *
 * @param other Target of attack
 * @param attackPowerDivider Value, on which resulting damage should be divided
 * (for example dual weapon attack contains two hits, each should deal 50% damage)
 */
fun Actor.hit(other: Actor, attackPowerDivider: Int = 1): Hit {
    val isAvoided = calculateIsAvoided(this, other)
    //TODO Calculate PerfectShieldBlock

    if (isAvoided) return Hit(targetId = other.id, isAvoided = true)

    val isCritical = calculateIsCritical(this, other)
    val isBlocked = calculateIsBlocked(this, other)

    return Hit(
        targetId = other.id,
        damage = calculateAutoAttackDamage(
            this, other, isCritical, isBlocked, usedSoulshot = false
        ) / attackPowerDivider,
        usedSoulshot = false, //TODO
        isCritical = isCritical,
        isBlocked = isBlocked,
    )
}

/** Calculates if attack of [attacker] on [attacked] is critical */
private fun calculateIsCritical(attacker: Actor, attacked: Actor): Boolean {
    var critRate = attacker.stats.critRate

    if (attacker.isOnSideOf(attacked))
        critRate = (critRate * CRIT_RATE_FROM_SIDE_MODIFIER).roundToInt()
    if (attacker.isBehind(attacked))
        critRate = (critRate * CRIT_RATE_FROM_BACK_MODIFIER).roundToInt()

    return critRate > Random.nextInt(0, 1000)
}

/** Calculates if [attacked] has avoided [attacker]'s attack */
private fun calculateIsAvoided(attacker: Actor, attacked: Actor): Boolean {
    var hitChance = EVASION_CHANCE_BASE + 2 * (attacker.stats.accuracy - attacked.stats.evasion)

    if (attacker.isOnSideOf(attacked))
        hitChance = (hitChance * ACCURACY_FROM_SIDE_MODIFIER).roundToInt()
    if (attacker.isBehind(attacked))
        hitChance = (hitChance * ACCURACY_FROM_BACK_MODIFIER).roundToInt()

    return hitChance < Random.nextInt(0, 100)
}

/** Calculates if [attacked] has blocked [attacker]'s attack  */
private fun calculateIsBlocked(attacker: Actor, attacked: Actor): Boolean {
    val attackerWeaponBonus = if (!attacked.hasShield) 0 else when (attacker.weaponType) {
        WeaponType.BOW -> BONUS_SHIELD_DEF_RATE_AGAINST_BOW
        WeaponType.DAGGER -> BONUS_SHIELD_DEF_RATE_AGAINST_DAGGER
        else -> 0
    }

    val blockChance = attacked.stats.shieldDefRate /* TODO * buff shield rate */ + attackerWeaponBonus
    return blockChance > Random.nextInt(0, 100)
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
