package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats

/**
 * Character class data - stats, skills, misc. information
 *
 * @property id Class identifier
 * @property requiredLevel Level, required to take this class. IMPORTANT for proper stats calculation
 * @property combatStats Initial values of character's basic stats
 * @property basicStats Initial values of character's stats
 * @property tradeAndInventoryStats Initial values for character trading stats
 * @property emptySlotStats Stats of character's empty slots. Will be applied if no item equipped in slot
 * @property perLevelGain CP, HP and MP per level gain coefficients
 * @property parentClass Parent class. For example, in L2 for 'Duelist' class parent class will be 'Gladiator'
 * @property characterTemplate Template for this class character creation
 * @property skillTree Map of skills to learn. Key - character level, value - list of skills, available at this level
 */
data class CharacterClass(
    override val id: Int,
    val requiredLevel: Int,
    val combatStats: CombatStats,
    val basicStats: BasicStats,
    val tradeAndInventoryStats: TradeAndInventoryStats,
    val emptySlotStats: Map<Slot, CombatStats>,
    val perLevelGain: PerLevelGain,
    val hpRegenPer10Levels: List<Double> = DEFAULT_HP_REGEN_PER_10_LEVELS,
    val mpRegenPer10Levels: List<Double> = DEFAULT_MP_REGEN_PER_10_LEVELS,
    val parentClass: CharacterClass? = null,
    val characterTemplate: CharacterTemplate? = null,
    val skillTree: Map<Int, List<SkillToLearn>>
): GameData {

    /**
     * Empty slots in game have stats too - for example, if no weapon equipped,
     * character hits will deal some damage anyway.
     */
    object DefaultEmptySlotStats {

        /**
         *  Default empty slot stats for fighter classes.
         *  Key - slot, value - stats of this slot
         */
        @JvmStatic
        val FIGHTER = mapOf(
            Slot.RIGHT_HAND to CombatStats(
                pAtk = 4,
                mAtk = 6,
                critRate = 44,
                atkSpd = 300,
            ),

            Slot.HEADGEAR to CombatStats(pDef = 12),
            Slot.UPPER_BODY to CombatStats(pDef = 31),
            Slot.LOWER_BODY to CombatStats(pDef = 18),
            Slot.GLOVES to CombatStats(pDef = 8),
            Slot.BOOTS to CombatStats(pDef = 7),

            Slot.RIGHT_RING to CombatStats(mDef = 5),
            Slot.LEFT_RING to CombatStats(mDef = 5),
            Slot.RIGHT_EARRING to CombatStats(mDef = 9),
            Slot.LEFT_EARRING to CombatStats(mDef = 9),
            Slot.NECKLACE to CombatStats(mDef = 13),
        )

        /**
         *  Default empty slot stats for mystic classes.
         *  Key - slot, value - stats of this slot
         */
        @JvmStatic
        val MYSTIC = mapOf(
            Slot.RIGHT_HAND to CombatStats(
                pAtk = 4,
                mAtk = 6,
                critRate = 44,
                atkSpd = 300,
            ),

            Slot.HEADGEAR to CombatStats(pDef = 12),
            Slot.UPPER_BODY to CombatStats(pDef = 15),
            Slot.LOWER_BODY to CombatStats(pDef = 8),
            Slot.GLOVES to CombatStats(pDef = 8),
            Slot.BOOTS to CombatStats(pDef = 7),

            Slot.RIGHT_RING to CombatStats(mDef = 5),
            Slot.LEFT_RING to CombatStats(mDef = 5),
            Slot.RIGHT_EARRING to CombatStats(mDef = 9),
            Slot.LEFT_EARRING to CombatStats(mDef = 9),
            Slot.NECKLACE to CombatStats(mDef = 13),
        )
    }

    companion object {

        /** Basic HP regeneration depends on character level and raises each 10 levels. */
        @JvmStatic
        val DEFAULT_HP_REGEN_PER_10_LEVELS = listOf(2.0, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5)

        /** Basic MP regeneration depends on character level and raises each 10 levels. */
        @JvmStatic
        val DEFAULT_MP_REGEN_PER_10_LEVELS = listOf(0.9, 1.2, 1.5, 1.8, 2.1, 2.4, 2.7, 3.0)

    }

    /**
     * Returns base class identifier.
     * For example, in L2 for 'Duelist' class base class will be 'Human Fighter'
     */
    val baseClassId: Int get() = if (this.parentClass == null) id else this.parentClass.baseClassId

    val baseAtkSpd = emptySlotStats.values.reduce { acc, stats -> acc + stats }.atkSpd
    val baseSpeed = combatStats.speed

    object Registry: GameDataRegistry<CharacterClass>()
}

/**`
 * Coefficients for max CP, HP and MP calculation according to character's level
 */
data class PerLevelGain(
    val cpAdd: Double = 0.0,
    val cpMod: Double = 0.0,
    val hpAdd: Double = 0.0,
    val hpMod: Double = 0.0,
    val mpAdd: Double = 0.0,
    val mpMod: Double = 0.0
)

/**
 * Requirements to learn skill with ID [skillId]
 *
 * @property skillId skill identifier
 * @property skillLevel Level of skill to be learned
 * @property spCost How many sp must be spent to learn this skill
 * @property autoLearn Should this skill be learnt on level up automatically
 * (in this case [spCost] will be ignored
 */
data class SkillToLearn(
    val skillId: Int,
    val skillLevel: Int,
    val spCost: Int = 0,
    val autoLearn: Boolean = false
)
