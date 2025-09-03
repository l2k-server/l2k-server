@file:JvmName("CalculationUtils")
package org.l2kserver.game.model.utils

import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.item.template.WeaponType
import kotlin.math.roundToInt
import kotlin.random.Random

const val PHYSICAL_ATTACK_BASE = 70
const val PHYSICAL_DMG_FROM_SIDE_MODIFIER = 1.1
const val PHYSICAL_DMG_FROM_BACK_MODIFIER = 1.2

const val ACCURACY_FROM_SIDE_MODIFIER = 1.1
const val ACCURACY_FROM_BACK_MODIFIER = 1.3

const val CRIT_RATE_FROM_SIDE_MODIFIER = 1.1
const val CRIT_RATE_FROM_BACK_MODIFIER = 1.2

const val EVASION_CHANCE_BASE = 88

const val BONUS_SHIELD_DEF_RATE_AGAINST_BOW = 30
const val BONUS_SHIELD_DEF_RATE_AGAINST_DAGGER = 12

/** Calculates if attack of [attacker] on [attacked] is critical */
fun calculateIsCritical(attacker: Actor, attacked: Actor): Boolean {
    val critRate = attacker.stats.critRate * calculatePositionCritChanceMultiplier(attacker, attacked)
    return critRate.roundToInt() > Random.nextInt(0, 1000)
}

/** Calculates if [attacked] has avoided [attacker]'s attack */
fun calculateIsAvoided(attacker: Actor, attacked: Actor): Boolean {
    var hitChance = EVASION_CHANCE_BASE + 2.0 * (attacker.stats.accuracy - attacked.stats.evasion)

    if (attacker.isOnSideOf(attacked)) hitChance *= ACCURACY_FROM_SIDE_MODIFIER
    if (attacker.isBehind(attacked)) hitChance *= ACCURACY_FROM_BACK_MODIFIER

    return hitChance.roundToInt() < Random.nextInt(0, 100)
}

/** Calculates if [attacked] has blocked [attacker]'s attack  */
fun calculateIsBlocked(attacker: Actor, attacked: Actor): Boolean {
    val attackerWeaponBonus = if (!attacked.hasShield) 0 else when (attacker.weaponType) {
        WeaponType.BOW -> BONUS_SHIELD_DEF_RATE_AGAINST_BOW
        WeaponType.DAGGER -> BONUS_SHIELD_DEF_RATE_AGAINST_DAGGER
        else -> 0
    }

    val blockChance = attacked.stats.shieldDefRate /* TODO * buff shield rate */ + attackerWeaponBonus

    return blockChance > Random.nextInt(0, 100)
}

/** Calculates critical hit chance multiplier, according to position and elevation factors*/
fun calculatePositionCritChanceMultiplier(attacker: Actor, attacked: Actor): Double {
    val elevationFactor = 0.008 * (attacker.position.z - attacked.position.z).coerceIn(-25..25) + 1.1
    val positionBonus = when {
        attacker.isBehind(attacked) -> CRIT_RATE_FROM_BACK_MODIFIER
        attacker.isOnSideOf(attacked) ->  CRIT_RATE_FROM_SIDE_MODIFIER
        else -> 1.0
    }

    return elevationFactor * positionBonus
}
