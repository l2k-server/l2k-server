package org.l2kserver.game.model.skill

import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.skill.action.SkillAction

/**
 * Skill instance
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillType Skill type - active, passive or toggle
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumes Skill consumables - mp, items, etc.
 * @property skillAction Effects, dealt by this skill
 */
interface SkillInstance {
    val skillId: Int
    val skillName: String
    val skillLevel: Int
    val skillType: SkillType
    val targetType: SkillTargetType
    val reuseDelay: Int
    val castTime: Int
    val repriseTime: Int
    val castRange: Int
    val effectRange: Int
    val requires: SkillRequirements?
    val consumes: SkillConsumables?
    val skillAction: SkillAction

    fun castsOnCorpse() = targetType == SkillTargetType.DEAD_ENEMY || targetType == SkillTargetType.DEAD_FRIEND
}

/** Skill type - active, magic, passive or toggle */
enum class SkillType {
    ACTIVE,
    MAGIC,
    PASSIVE,
    TOGGLE
}

/**
 * Type of target, the skill can be used on.
 * This tells on which target type skill will be <strong>cast</strong>,
 * effects has their own target types
 */
enum class SkillTargetType {
    /**
     * Skill will be cast on actor's target enemy.
     * These skills can be applied to friendly targets only with `forced` parameter
     */
    ENEMY,

    /**
     * Skill will be cast on actor's target 'friend' - summon, other non-PK player, friendly NPC, etc.
     * These skills can be applied to enemy targets only with `forced` parameter
     */
    FRIEND,

    /**
     * Skill will be cast on actor's target enemy corpse (like Necromancer's Summon Zombie).
     */
    DEAD_ENEMY,

    /**
     * Skill will be cast on actor's target friend corpse (like Resurrection).
     */
    DEAD_FRIEND,

    /**
     * Skill will be cast on the actor himself
     * These skills can be applied to friendly targets only with `forced` parameter
     */
    SELF
}

/**
 * Skill consumables
 *
 * @property hp - How much HP is spent to cast skill
 * @property mp - How much mana is spent to cast skill
 * @property item - How much and which item is consumed to use skill
 */
data class SkillConsumables(
    val hp: Int,
    val mp: Int,
    val item: ConsumableItem?
)
