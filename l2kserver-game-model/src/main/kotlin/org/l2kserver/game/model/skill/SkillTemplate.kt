package org.l2kserver.game.model.skill

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.WeaponType

/**
 * Data class representing common skill data
 *
 * @property id Skill identifier
 * @property skillName Skill name (eng)
 * @property skillType - Skill type - active, passive or toggle
 * @property reuseDelay - Base cooldown of this skill
 * @property castTime - Base casting time of this skill
 * @property castRange - Range to target to cast this skill, or radius for mass skill
 * @property effectRange - TODO
 * @property requirements - Requirements to use this skill
 * @property maxSkillLevel - Max level of this skill to be learnt
 * @property consumes - Skill consumables - mp, items, etc.
 * @property effects - Effects, dealt by this skill
 */
data class SkillTemplate(
    override val id: Int,
    val skillName: String,
    val skillType: SkillType,
    val targetType: SkillTargetType,
    val reuseDelay: Int,
    val castTime: Int,
    val castRange: Int = 0,
    val effectRange: Int = 0,
    val requirements: SkillRequirements? = null,
    val maxSkillLevel: Int,
    val consumes: SkillConsumablesTemplate? = null,
    val effects: List<SkillEffectTemplate>
): GameData {

    object Registry: GameDataRegistry<SkillTemplate>()
}

/**
 * Skill type - active, passive or toggle
 */
enum class SkillType {
    ACTIVE,
    MAGIC,
    PASSIVE,
    TOGGLE
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
 * @property mp - How much mana is spent to cast skill on each level (Note: skill level starts with 1)
 */
data class SkillConsumablesTemplate(
    val mp: List<Int>
)

/**
 * Effect dealt by skill
 *
 * @property effectType Type of effect - physical damage, magical damage, buff, etc.
 * @property targetType Target that the effect is applied to - Target, Self, Party, etc. If null,
 * global targetType of skill itself will be used
 * @property power Power of skill on each level (Note: skill level starts with 1)
 */
data class SkillEffectTemplate(
    val effectType: SkillEffectType,
    val targetType: SkillTargetType?,
    val power: List<Int>?
)

/**
 * Type of skill effect
 *
 * @property PHYS_DAMAGE Deal physical damage to effect targets
 */
enum class SkillEffectType {
    PHYS_DAMAGE
}

/**
 * Targets that the effect is applied to
 */
enum class SkillTargetType {
    /** Effect will be applied to actor's target */
    ENEMY
}
