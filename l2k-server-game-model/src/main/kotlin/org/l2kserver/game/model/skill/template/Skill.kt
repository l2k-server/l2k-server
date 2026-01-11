package org.l2kserver.game.model.skill.template

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.effect.AbnormalEffect
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

abstract class PassiveSkill: Skill {
    /** Calculates abnormal effects, applied by this skill */
    abstract fun effect(actor: ActorInstance, actionLevel: Int): AbnormalEffect
}

abstract class ToggleSkill: Skill {
    /** Requirements to use this skill */
    abstract val requires: SkillRequirements?
}

/**
 * Active skill template
 *
 * @property targetType Type of target to cast this skill on
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumes Skill consumables - mp, items, etc.
 * @property overhitPossible Can this skill produce an over-hit
 * @property forcedUsageAllowed Can this skill be used to incorrect target with CTRL
 * @property usesCasterStats Should character stats be used at skill casting time, cast range, etc. calculations
 */
abstract class ActiveSkill: Skill {
    abstract val targetType: SkillTargetType
    abstract val reuseDelay: Int
    abstract val castTime: Int
    abstract val isMagic: Boolean
    open val repriseTime: Int = 0
    open val castRange: Int = 0
    open val effectRange: Int = 0
    open val requires: SkillRequirements? = null
    open val consumesToStart: SkillConsumablesTemplate? = null
    open val consumes: SkillConsumablesTemplate? = null
    open val overhitPossible: Boolean = false
    open val forcedUsageAllowed: Boolean = true
    open val usesCasterStats: Boolean = true

    abstract fun affect(context: SkillContext): Effects
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
