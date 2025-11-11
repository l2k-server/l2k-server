package org.l2kserver.game.extensions.model.stats

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.CombatStatsMultipliers
import kotlin.collections.fold
import kotlin.collections.plus
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAX_CRIT_RATE = 500
private const val P_DEF_BASE = 4

private const val REGENERATION_MULTIPLIER_ON_SITTING = 1.5
private const val REGENERATION_MULTIPLIER_ON_STAYING = 1.1
private const val REGENERATION_MULTIPLIER_ON_RUNNING = 0.7

/**
 * Apply stats of the equipment
 *
 * @param character Equipment owner
 */
fun CombatStats.applyEquipmentOf(character: PlayerCharacter): CombatStats {
    var result = this + (character.inventory.weapon?.stats ?: character.characterClass.emptySlotStats[Slot.RIGHT_HAND])

    val upperBodyStats =
        character.inventory[Slot.UPPER_BODY]?.stats ?: character.characterClass.emptySlotStats[Slot.UPPER_BODY]
    val lowerBodyStats =
        character.inventory[Slot.LOWER_BODY]?.stats ?: character.characterClass.emptySlotStats[Slot.LOWER_BODY]

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

/** Calculate stats after applying base stats and level modifiers */
fun CombatStats.applyModifiersOf(character: PlayerCharacter): CombatStats {
    val level = character.level
    val characterClass = character.characterClass
    val basicStats = character.basicStats

    val passiveEffects = character.skillsAndMagic.passives()
        .mapNotNull { it.effect(character).getCombatStatsMultipliers(character) }

    val temporalEffects = character.temporalEffects
        .mapNotNull { it.getCombatStatsMultipliers(character) }

    val multipliers = (passiveEffects + temporalEffects).fold(CombatStatsMultipliers()) { acc, multipliers ->
        acc * multipliers
    }

    val levelModifier = (character.level + 89) / 100.0

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
        maxCp = (maxCpBase * basicStats.con.cpModifier * multipliers.maxCp).toInt(),
        maxHp = (maxHpBase * basicStats.con.hpModifier * multipliers.maxHp).toInt(),
        maxMp = (maxMpBase * basicStats.men.mpModifier * multipliers.maxMp).toInt(),

        pAtk = (this.pAtk * basicStats.str.pAtkModifier * levelModifier * multipliers.pAtk).toInt(),
        pDef = P_DEF_BASE + (this.pDef * levelModifier * multipliers.pDef).toInt(),
        accuracy = this.accuracy + basicStats.dex.accuracyBonus + level,
        critRate = (this.critRate * basicStats.dex.critRateModifier * multipliers.critRate).toInt(),
        atkSpd = (this.atkSpd * basicStats.dex.atkSpdModifier * multipliers.atkSpd).toInt(),

        mAtk = (this.mAtk * basicStats.int.mAtkModifier.pow(2) * levelModifier.pow(2) * multipliers.mAtk).toInt(),
        mDef = (this.mDef * basicStats.men.mDefModifier * levelModifier * multipliers.mDef).toInt(),

        evasion = this.evasion + basicStats.dex.evasionBonus + level,
        speed = (this.speed * basicStats.dex.speedModifier * multipliers.speed).toInt(),
        castingSpd = (this.castingSpd * basicStats.wit.castingSpdModifier * multipliers.castingSpd).toInt(),

        shieldDef = (this.shieldDef * multipliers.shieldDef).toInt(),
        shieldDefRate = (this.shieldDefRate * basicStats.dex.shieldBlockRateModifier * multipliers.shieldDefRate)
            .toInt(),

        mCritRate = this.mCritRate + (basicStats.wit.magicCritChanceBonus * 10 * multipliers.mCritRate).roundToInt(),

        hpRegen = (this.hpRegen + hpRegenLevelModifier(characterClass, level))
                * basicStats.con.hpRegenModifier
                * multipliers.hpRegen,
        mpRegen = (this.mpRegen + mpRegenLevelModifier(characterClass, level))
                * basicStats.men.mpRegenModifier
                * multipliers.mpRegen,
        cpRegen = (this.cpRegen + cpRegenLevelModifier(characterClass, level))
                * basicStats.con.cpRegenModifier
                * multipliers.cpRegen
    )
}

/** Applies fixed bonus of items, buffs and passives */
fun CombatStats.applyFixedBonusStatsOf(character: PlayerCharacter): CombatStats {
    val itemsFixedBonusStats = character.inventory.findAllEquipped().mapNotNull { it.fixedBonusStats }

    val passivesFixedBonusStats = character.skillsAndMagic.passives()
        .mapNotNull { it.effect(character).getFixedBonusStats(character) }

    val temporalEffectsFixedBonusStats = character.temporalEffects
        .mapNotNull { it.getFixedBonusStats(character) }

    return this + (itemsFixedBonusStats + passivesFixedBonusStats + temporalEffectsFixedBonusStats)
        .fold(CombatStats()) { acc, stats -> acc + stats }
}

/** Applies posture bonus to regen stats */
fun CombatStats.applyPostureBonusOf(actor: ActorInstance): CombatStats {
    val postureBonus = when {
        actor is PlayerCharacter && actor.posture == Posture.SITTING -> REGENERATION_MULTIPLIER_ON_SITTING
        !actor.isMoving -> REGENERATION_MULTIPLIER_ON_STAYING
        actor.isRunning -> REGENERATION_MULTIPLIER_ON_RUNNING
        else -> 1.0
    }

    return this.copy(
        cpRegen = this.cpRegen * postureBonus,
        hpRegen = this.hpRegen * postureBonus,
        mpRegen = this.mpRegen * postureBonus
    )
}

/** Calculate stats after applying limitations */
fun CombatStats.applyLimitations(): CombatStats = this.copy(
    critRate = minOf(this.critRate, MAX_CRIT_RATE)
)

/** Calculates stats after applying buffs, debuffs and passives */
fun CombatStats.applyAbnormalsOf(npc: NpcInstance): CombatStats {
    val buffFixedBonusStats = npc.temporalEffects.map { it.getFixedBonusStats(npc) }
    return this + buffFixedBonusStats.fold(CombatStats()) { acc, stats -> acc + stats }
}

private fun hpRegenLevelModifier(characterClass: CharacterClass, level: Int) =
    characterClass.hpRegenPer10Levels[level / 10]

private fun mpRegenLevelModifier(characterClass: CharacterClass, level: Int) =
    characterClass.mpRegenPer10Levels[level / 10]

private fun cpRegenLevelModifier(characterClass: CharacterClass, level: Int) =
    hpRegenLevelModifier(characterClass, level)

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
