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
import org.l2kserver.game.model.skill.instance.SkillTargetType

/** Stores all the skill templates */
object SkillTemplateRegistry: GameDataRegistry<SkillTemplate>()

/**
 * Interface, representing skill template
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property maxLevel Maximum level of skill, available to learn
 */
sealed interface SkillTemplate: GameData {
    val skillName: String
    val maxLevel: Int
}

interface PassiveSkillTemplate: SkillTemplate {
    /** Calculates abnormal effects, applied by this skill */
    fun effect(actor: ActorInstance, actionLevel: Int): AbnormalEffect
}

interface ToggleSkillTemplate: SkillTemplate {
    /** Requirements to use this skill */
    val requires: SkillRequirements?
}

/**
 * Skill, that can be caster
 *
 * @property targetType - Type of target to cast this skill on
 * @property reuseDelay - Base cooldown of this skill
 * @property castTime - Base casting time of this skill
 * @property repriseTime - Time to return to the starting position after skill casting
 * @property castRange - Range to target to cast this skill, or radius for mass skill
 * @property effectRange - TODO
 * @property requires - Requirements to use this skill
 * @property consumes - Skill consumables - mp, items, etc.
 */
sealed interface CastableSkillTemplate: SkillTemplate {
    val targetType: SkillTargetType
    val reuseDelay: Int
    val castTime: Int
    val repriseTime: Int get() = 0
    val castRange: Int get() = 0
    val effectRange: Int get() = 0
    val requires: SkillRequirements? get() = null
    val consumesToStart: SkillConsumablesTemplate? get() = null
    val consumes: SkillConsumablesTemplate? get() = null
    val overhitPossible: Boolean get() = false

    fun affect(context: SkillContext): Effects
}

/** Active (physical) skill template */
interface ActiveSkillTemplate: CastableSkillTemplate

/** Magic skill template */
interface MagicSkillTemplate: CastableSkillTemplate

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
)
