package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.AttackRange
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats

/** Stores all the character classes */
object CharacterClassRegistry: GameDataRegistry<CharacterClass>()

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
            attackRange = AttackRange.MELEE_WEAPON_DEFAULT_ATTACK_RANGE,
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

/**
 * Character class data - stats, skills, misc. information
 *
 * @property id Class identifier
 * @property requiredLevel Level, required to take this class. IMPORTANT for proper stats calculation
 * @property basicStats Initial values of character's basic stats
 * @property tradeAndInventoryStats Initial values for character trading stats
 * @property emptySlotStats Stats of character's empty slots. Will be applied if no item equipped in slot
 * @property parentClass Parent class. For example, in L2 for 'Duelist' class parent class will be 'Gladiator'
 * @property characterTemplate Template for this class character creation
 * @property skillTree Map of skills to learn. Key - character level, value - list of skills, available at this level
 * @property baseClassId Base class identifier (for example, for Duelist base class will be Human Fighter)
 * @property baseAtkSpd Base attack speed of this class.
 * This value must be the same as on client side - it is necessary for proper animations
 * @property baseSpeed Base movement speed of this class.
 * This value must be the same as on client side - it is necessary for proper animations
 */
abstract class CharacterClass: GameData {
    abstract override val id: Int

    abstract val requiredLevel: Int
    abstract val basicStats: BasicStats
    abstract val tradeAndInventoryStats: TradeAndInventoryStats
    open val emptySlotStats: Map<Slot, CombatStats> get() = emptyMap()

    open val parentClass: CharacterClass? get() = null
    open val characterTemplate: CharacterTemplate? get() = parentClass?.characterTemplate ?: error(
        "Cannot find character template for class '${this::class.qualifiedName}' Was it defined in base class?"
    )

    abstract val skillTree: Map<Int, List<SkillToLearn>>

    val baseClassId: Int get() = this.parentClass?.baseClassId ?: this.id

    abstract val baseAtkSpd: Int
    abstract val baseSpeed: Int

    /** Initial values of character's combat stats, based on character's level */
    abstract fun getCombatStats(characterLevel: Int): CombatStats

    override fun validate() {
        check(parentClass != this) {
            "Character class cannot be base class for itself. For base class define parent class = null"
        }
        check(parentClass != null || characterTemplate != null) {
            "Either parent class or character template must be defined"
        }
    }

    companion object {

        /**
         * Basic HP regeneration depends on character level and raises each 10 levels. (1 - 10, 11 - 20, etc.)
         *
         * Note: array is zero-base, level should be decremented
         */
        val DEFAULT_HP_REGEN_PER_10_LEVELS = listOf(2.0, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5)

        /**
         * Basic MP regeneration depends on character level and raises each 10 levels. (1 - 10, 11 - 20, etc.)
         *
         * Note: array is zero-base, level should be decremented
         */
        val DEFAULT_MP_REGEN_PER_10_LEVELS = listOf(0.9, 1.2, 1.5, 1.8, 2.1, 2.4, 2.7, 3.0)

        /**
         * Basic CP regeneration depends on character level and raises each 10 levels. (1 - 10, 11 - 20, etc.)
         *
         * Note: array is zero-base, level should be decremented
         */
        val DEFAULT_CP_REGEN_PER_10_LEVELS = DEFAULT_HP_REGEN_PER_10_LEVELS

    }
}

/**
 * Requirements to learn skill with ID [skillId]
 *
 * @property skillId skill identifier
 * @property skillLevel Level of skill to be learned
 * @property spCost How many sp must be spent to learn this skill
 * @property autoLearn Should this skill be learnt on level up automatically (in this case [spCost] will be ignored)
 */
data class SkillToLearn(
    val skillId: Int,
    val skillLevel: Int,
    val spCost: Int = 0,
    val autoLearn: Boolean = false
)
