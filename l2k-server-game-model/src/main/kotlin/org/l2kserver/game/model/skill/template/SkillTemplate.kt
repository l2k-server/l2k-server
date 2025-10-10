package org.l2kserver.game.model.skill.template

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.action.ActiveSkillAction
import org.l2kserver.game.model.skill.action.AbnormalSkillAction
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.instance.ActiveSkillType

/** Stores all the skill templates */
object SkillTemplateRegistry: GameDataRegistry<SkillTemplate>()

sealed interface SkillTemplate: GameData {
    val skillName: String
    val maxLevel: Int
}

/**
 * Data class representing active (physical) skill
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property maxLevel - maximum level of this skill
 * @property skillType - Skill type - active or magic
 * @property reuseDelay - Base cooldown of this skill
 * @property castTime - Base casting time of this skill
 * @property repriseTime - Time to return to the starting position after skill casting
 * @property castRange - Range to target to cast this skill, or radius for mass skill
 * @property effectRange - TODO
 * @property requires - Requirements to use this skill
 * @property consumes - Skill consumables - mp, items, etc.
 * @property skillAction - Actions, performed by this skill
 */
data class ActiveSkillTemplate(
    override val id: Int,
    override val skillName: String,
    override val maxLevel: Int,
    val skillType: ActiveSkillType,
    val targetType: SkillTargetType,
    val reuseDelay: Int,
    val castTime: Int,
    val repriseTime: Int = 0,
    val castRange: Int = 0,
    val effectRange: Int = 0,
    val requires: SkillRequirements? = null,
    val consumesToStart: SkillConsumablesTemplate? = null,
    val consumes: SkillConsumablesTemplate? = null,
    val overhitPossible: Boolean = false,
    val lethalStrikePossible: Boolean = false,
    val skillAction: ActiveSkillAction
): SkillTemplate

/**
 * Data class representing passive skill
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property maxLevel - maximum level of this skill
 * @property skillAction Actions, performed by this skill
 */
data class PassiveSkillTemplate(
    override val id: Int,
    override val skillName: String,
    override val maxLevel: Int,
    val skillAction: AbnormalSkillAction
): SkillTemplate

/**
 * Data class representing toggle skill
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property maxLevel - maximum level of this skill
 * @property requires - Requirements to use this skill
 */
data class ToggleSkillTemplate(
    override val id: Int,
    override val skillName: String,
    override val maxLevel: Int,
    val requires: SkillRequirements?,
): SkillTemplate

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
 * (Note: skill level starts with 1)
 * @property hp - How much HP is spent to cast skill on each skill level. Consumed AFTER skill casting
 * (Note: skill level starts with 1)
 * @property mp - How much mana is spent to cast skill on each skill level. Consumed AFTER skill casting
 * (Note: skill level starts with 1)
 * @property item - Which item is spent to cast skill on each skill level
 * (Note: skill level starts with 1)
 */
data class SkillConsumablesTemplate(
    val hp: List<Int>? = null,
    val mp: List<Int>? = null,
    val item: List<ConsumableItem?>? = null
)
