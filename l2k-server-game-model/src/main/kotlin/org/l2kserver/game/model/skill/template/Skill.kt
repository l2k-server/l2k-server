package org.l2kserver.game.model.skill.template

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.ArmorType
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalEffect
import org.l2kserver.game.model.skill.effect.Effect
import org.l2kserver.game.model.skill.instance.SkillConsumables
import org.l2kserver.game.model.skill.instance.SkillTargetType

/** Stores all the skill templates */
object SkillRegistry: GameDataRegistry<Skill>()

/**
 * Interface, representing skill template
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property maxLevel Maximum level of skill, available to learn
 */
sealed interface Skill: GameData {
    val skillName: String
    val maxLevel: Int get() = 1
}

interface PassiveSkill: Skill {
    /** Calculates abnormal effects, applied by this skill */
    fun effect(actor: ActorInstance, actionLevel: Int): AbnormalEffect
}

/**
 * Active skill template
 *
 * @property targetType Type of target to cast this skill on
 * @property reuseDelay Base cooldown of this skill (millis)
 * @property castTime Base casting time of this skill (millis)
 * @property repriseTime Time to return to the starting position after skill casting (millis)
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumes Skill consumables - mp, items, etc.
 * @property overhitPossible Can this skill produce an over-hit
 * @property forcedUsageAllowed Can this skill be used to incorrect target with CTRL
 * @property usesCasterStats Should character stats be used at skill casting time, cast range, etc. calculations
 */
interface ActiveSkill: Skill {
    val targetType: SkillTargetType
    val reuseDelay: Int
    val castTime: Int
    val isMagic: Boolean
    val repriseTime: Int get() = 0
    val castRange: Int get() = 0
    val effectRange: Int get() = 0
    val requires: SkillRequirements? get() = null
    val consumesToStart: SkillConsumablesTemplate? get() = null
    val consumes: SkillConsumablesTemplate? get() = null
    val overhitPossible: Boolean get() = false
    val forcedUsageAllowed: Boolean get() = true
    val usesCasterStats: Boolean get() = true

    /**
     * Checks if this skill can be used
     *
     * @return null if all checks passed, or [SkillConditionFailed] with additional info why skill cannot be used
     */
    fun canBeUsed(caster: ActorInstance, target: ActorInstance): SkillConditionFailed? { return null }

    /**
     * Calculates skill effects
     *
     * @param context Context of skill usage (see [SkillContext])
     * @return Iterable of Effects, that were produced by this skill
     */
    fun affect(context: SkillContext): Iterable<Effect>
}

/**
 * Why does the skill cannot be used
 *
 * @property message System message text to send to the client
 **/
open class SkillConditionFailed(val message: String? = null) {

    /** Skill cannot be used because target is already resurrected by someone else */
    object TargetIsPendingResurrection: SkillConditionFailed()
}

/**
 * Skill requirements - conditions to use this skill
 *
 * @property weaponTypes Types of weapon, required to use this skill. If null - all weapon types allowed
 * @property armorTypes Types of armor, required to use this skill. If null - all armor types allowed
 */
data class SkillRequirements(
    val weaponTypes: List<WeaponType>? = null,
    val armorTypes: List<ArmorType>? = null
)

/**
 * Skill consumables
 *
 * @property hp - How much HP is spent to cast skill on each skill level
 * (Note: skill level starts with 1)
 * @property mp - How much mana is spent to cast skill on each skill level
 * (Note: skill level starts with 1)
 * @property item - Which item is spent to cast skill on each skill level
 * (Note: skill level starts with 1)
 */
data class SkillConsumablesTemplate(
    val hp: List<Int>? = null,
    val mp: List<Int>? = null,
    val item: List<ConsumableItem?>? = null
) {

    /** Constructor for single-level skill consumables */
    constructor(hp: Int? = null, mp: Int? = null, item: ConsumableItem? = null): this(
        hp = hp?.let { listOf(it) },
        mp = mp?.let { listOf(it) },
        item = item?.let { listOf(it) }
    )

    /**
     * Get skill consumption by [skillLevel]
     *
     * @throws IllegalArgumentException if no data about skill consumption at [skillLevel] exists
     */
    fun getByLevel(skillLevel: Int) = SkillConsumables(
        hp = this.hp?.let {
            requireNotNull(it.getOrNull(skillLevel - 1)) {
                "No data about hp consumption at skill level = '$skillLevel' found"
            }
        } ?: 0,
        mp = this.mp?.let {
            requireNotNull(it.getOrNull(skillLevel - 1)) {
                "No data about mp consumption at skill level = '$skillLevel' found"
            }
        } ?: 0,
        item = this.item?.let {
            requireNotNull(it.getOrNull(skillLevel - 1)) {
                "No data about item consumption at skill level = '$skillLevel' found"
            }
        },
    )

}
