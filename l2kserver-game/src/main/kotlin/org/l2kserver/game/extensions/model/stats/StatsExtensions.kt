package org.l2kserver.game.extensions.model.stats

import org.l2kserver.game.domain.character.CharacterClass
import org.l2kserver.game.domain.item.template.Slot
import org.l2kserver.game.domain.item.template.WeaponType
import org.l2kserver.game.model.PaperDoll
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.Stats
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAX_CRIT_RATE = 500

private const val MELEE_WEAPON_DEFAULT_ATTACK_RANGE = 40
private const val BOW_DEFAULT_ATTACK_RANGE = 450

/**
 * Apply stats of the equipment
 *
 * @param paperDoll Equipped items
 * @param characterClass Character class - contains stats of empty slots
 */
fun Stats.applyEquipment(paperDoll: PaperDoll, characterClass: CharacterClass): Stats {
    var result = this

    var weaponStats = paperDoll.getWeapon()?.stats ?: characterClass.emptySlotStats[Slot.RIGHT_HAND]

    result += weaponStats?.applyDefaultRange(paperDoll.getWeapon()?.type ?: WeaponType.FIST)

    val upperBodyStats = paperDoll[Slot.UPPER_BODY]?.stats ?: characterClass.emptySlotStats[Slot.UPPER_BODY]
    val lowerBodyStats = paperDoll[Slot.LOWER_BODY]?.stats ?: characterClass.emptySlotStats[Slot.LOWER_BODY]

    result += paperDoll[Slot.UPPER_AND_LOWER_BODY]?.stats ?: (upperBodyStats?.plus(lowerBodyStats))

    val slotsLeft = Slot.entries - listOf(
        Slot.RIGHT_HAND,
        Slot.TWO_HANDS,
        Slot.UPPER_BODY,
        Slot.LOWER_BODY,
        Slot.UPPER_AND_LOWER_BODY
    )
    slotsLeft.forEach {
        result += paperDoll[it]?.stats ?: characterClass.emptySlotStats[it]
    }

    return result
}

/**
 * Calculate stats after applying base stats and level modifiers
 */
fun Stats.applyModifiers(level: Int, characterClass: CharacterClass, basicStats: BasicStats): Stats {
    val levelModifier = (level + 89) / 100.0

    val levelResourceMultiplier = level - characterClass.requiredLevel
    val maxCpBase = this.maxCp + calculateResourceStatLevelBonus(
        levelResourceMultiplier, characterClass.perLevelGain.cpAdd, characterClass.perLevelGain.cpMod
    )
    val maxHpBase = this.maxHp + calculateResourceStatLevelBonus(
        levelResourceMultiplier, characterClass.perLevelGain.hpAdd, characterClass.perLevelGain.hpMod
    )
    val maxMpBase = this.maxMp + calculateResourceStatLevelBonus(
        levelResourceMultiplier, characterClass.perLevelGain.mpAdd, characterClass.perLevelGain.mpMod
    )

    return this.copy(
        maxCp = (maxCpBase * basicStats.con.cpModifier).toInt(),
        maxHp = (maxHpBase * basicStats.con.hpModifier).toInt(),
        maxMp = (maxMpBase * basicStats.men.mpModifier).toInt(),

        pAtk = (this.pAtk * basicStats.str.pAtkModifier * levelModifier).toInt(),
        pDef = (this.pDef * levelModifier).toInt(),
        accuracy = this.accuracy + basicStats.dex.accuracyBonus + level,
        critRate = (this.critRate * basicStats.dex.critRateModifier).toInt(),
        atkSpd = (this.atkSpd * basicStats.dex.atkSpdModifier).toInt(),
        mAtk = (this.mAtk * basicStats.int.mAtkModifier.pow(2) * levelModifier.pow(2)).toInt(),
        mDef = (this.mDef * basicStats.men.mDefModifier * levelModifier).toInt(),
        evasion = this.evasion + basicStats.dex.evasionBonus + level,
        speed = (this.speed * basicStats.dex.speedModifier).toInt(),
        castingSpd = (this.castingSpd * basicStats.wit.castingSpdModifier).toInt(),
        shieldDef = this.shieldDef,
        shieldDefRate = (this.shieldDefRate * basicStats.dex.shieldBlockRateModifier).toInt(),

        hpRegen = (this.hpRegen + hpRegenLevelModifier(level)) * basicStats.con.hpRegenModifier,
        mpRegen = (this.mpRegen + mpRegenLevelModifier(level)) * basicStats.men.mpRegenModifier,
        cpRegen = (this.cpRegen + cpRegenLevelModifier(level)) * basicStats.con.cpRegenModifier
    )
}

/**
 * Calculate stats after applying limitations
 */
fun Stats.applyLimitations(): Stats = this.copy(
    critRate = minOf(this.critRate, MAX_CRIT_RATE)
)

private fun hpRegenLevelModifier(level: Int) = if (level > 10) (level - 1).toDouble() / 10 else 0.5
private fun mpRegenLevelModifier(level: Int) = 0.3 * ((level - 1).toDouble() / 10)
private fun cpRegenLevelModifier(level: Int) = hpRegenLevelModifier(level)

/**
 * Calculate CP, HP pr MP level bonus
 *
 * @param levelMultiplier amount of levels got in current class
 * @param addition First magic coefficient for resource stat calculation
 * @param modifier Second magic coefficient for resource stat calculation
 *
 * @return Total resource stat bonus per level
 */
private fun calculateResourceStatLevelBonus(levelMultiplier: Int, addition: Double, modifier: Double): Int {
    val leveledModifier = modifier * levelMultiplier
    val bonusMax = (addition + leveledModifier) * levelMultiplier
    val bonusMin = (addition * levelMultiplier) + leveledModifier

    return ((bonusMax + bonusMin) / 2).roundToInt()
}

/**
 * Applies default attack range for weapon, if it was not provided in template.
 * For melee weapon - 40, for bow - 450.
 */
private fun Stats.applyDefaultRange(weaponType: WeaponType) = if (this.attackRange == 0) {
    when (weaponType) {
        WeaponType.BOW -> this + Stats(attackRange = BOW_DEFAULT_ATTACK_RANGE)
        else -> this + Stats(attackRange = MELEE_WEAPON_DEFAULT_ATTACK_RANGE)
    }
} else this
