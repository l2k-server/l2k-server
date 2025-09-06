package org.l2kserver.game.model.skill

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.skill.action.SkillAction

/**
 * Data class representing common skill data
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property skillType - Skill type - active, passive or toggle
 * @property reuseDelay - Base cooldown of this skill
 * @property castTime - Base casting time of this skill
 * @property repriseTime - Time to return to the starting position after skill casting
 * @property castRange - Range to target to cast this skill, or radius for mass skill
 * @property effectRange - TODO
 * @property requires - Requirements to use this skill
 * @property maxSkillLevel - Max level of this skill to be learnt
 * @property consumes - Skill consumables - mp, items, etc.
 * @property skillAction - Effects, dealt by this skill
 */
data class SkillTemplate(
    override val id: Int,
    val skillName: String,
    val skillType: SkillType,
    val targetType: SkillTargetType,
    val reuseDelay: Int,
    val castTime: Int,
    val repriseTime: Int = 0,
    val castRange: Int = 0,
    val effectRange: Int = 0,
    val requires: SkillRequirements? = null,
    val maxSkillLevel: Int,
    val consumes: SkillConsumablesTemplate? = null,
    val skillAction: SkillAction
): GameData {

    object Registry: GameDataRegistry<SkillTemplate>()
}

/**
 * Skill requirements - conditions to use this skill
 *
 * @property weaponTypes Types of weapon, required to use this skill. If null - all weapon types allowed
 */
data class SkillRequirements(
    val weaponTypes: List<WeaponType>? = null
)

/**
 * Skill consumables
 *
 * @property hp - How much HP is spent to cast skill on each level (Note: skill level starts with 1)
 * @property mp - How much mana is spent to cast skill on each level (Note: skill level starts with 1)
 * @property item - Which item is spent to cast skill on each level (Note: skill level starts with 1)
 */
data class SkillConsumablesTemplate(
    val hp: List<Int>? = null,
    val mp: List<Int>? = null,
    val item: List<ConsumableItem?>? = null
)

