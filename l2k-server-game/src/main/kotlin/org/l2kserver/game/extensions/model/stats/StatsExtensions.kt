package org.l2kserver.game.extensions.model.stats

import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAX_CRIT_RATE = 500
private const val P_DEF_BASE = 4

/**
 * Apply stats of the equipment
 *
 * @param character Equipment owner
 */
fun CombatStats.applyEquipment(character: PlayerCharacter): CombatStats {
    var result = this + (character.inventory.weapon?.stats ?: character.characterClass.emptySlotStats[Slot.RIGHT_HAND])

    val upperBodyStats = character.inventory[Slot.UPPER_BODY]?.stats ?: character.characterClass.emptySlotStats[Slot.UPPER_BODY]
    val lowerBodyStats = character.inventory[Slot.LOWER_BODY]?.stats ?: character.characterClass.emptySlotStats[Slot.LOWER_BODY]

    result += character.inventory[Slot.UPPER_AND_LOWER_BODY]?.stats ?: (upperBodyStats?.plus(lowerBodyStats))

    val slotsLeft = Slot.entries - listOf(
        Slot.RIGHT_HAND,
        Slot.TWO_HANDS,
        Slot.UPPER_BODY,
        Slot.LOWER_BODY,
        Slot.UPPER_AND_LOWER_BODY
    )
    slotsLeft.forEach {
        result += character.inventory[it]?.stats ?: character.characterClass.emptySlotStats[it]
    }

    return result
}

/**
 * Calculate stats after applying base stats and level modifiers
 */
fun CombatStats.applyModifiers(level: Int, characterClass: CharacterClass, basicStats: BasicStats): CombatStats {
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
        pDef = P_DEF_BASE + (this.pDef * levelModifier).toInt(),
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

        hpRegen = (this.hpRegen + hpRegenLevelModifier(characterClass, level)) * basicStats.con.hpRegenModifier,
        mpRegen = (this.mpRegen + mpRegenLevelModifier(characterClass, level)) * basicStats.men.mpRegenModifier,
        cpRegen = (this.cpRegen + cpRegenLevelModifier(characterClass, level)) * basicStats.con.cpRegenModifier
    )
}

/**
 * Calculate stats after applying limitations
 */
fun CombatStats.applyLimitations(): CombatStats = this.copy(
    critRate = minOf(this.critRate, MAX_CRIT_RATE)
)

private fun hpRegenLevelModifier(characterClass: CharacterClass, level: Int) = characterClass.hpRegenPer10Levels[level/10]
private fun mpRegenLevelModifier(characterClass: CharacterClass, level: Int) = characterClass.mpRegenPer10Levels[level/10]
private fun cpRegenLevelModifier(characterClass: CharacterClass, level: Int) = hpRegenLevelModifier(characterClass, level)

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
