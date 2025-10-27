package org.l2kserver.game.model.skill.instance

import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.skill.action.ActiveSkillAction
import org.l2kserver.game.model.skill.template.SkillRequirements
import java.time.Instant

/**
 * Skill instance
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillLevel Level of skill (learnt)
 * @property skillType Skill type - active or magic
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumesToStart
 * @property consumes Skill consumables - mp, items, etc.
 * @property skillAction Effects, dealt by this skill
 */
interface ActiveSkillInstance: SkillInstance {
    override val skillId: Int
    override val skillName: String
    override val skillLevel: Int
    val skillType: ActiveSkillType
    val targetType: SkillTargetType
    val reuseDelay: Int
    val castTime: Int
    val repriseTime: Int
    val castRange: Int
    val effectRange: Int
    val requires: SkillRequirements?
    val consumesToStart: SkillConsumables?
    val consumes: SkillConsumables?
    val overhitPossible: Boolean
    val skillAction: ActiveSkillAction
    val nextUsageTime: Instant
}

/** Skill type - active, magic, passive or toggle */
//TODO Separate Instance classes??
enum class ActiveSkillType {
    ACTIVE,
    MAGIC
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
     * Skill will be cast on actor's target monster corpse (like Necromancer's Summon Zombie).
     */
    DEAD_NPC,

    /**
     * Skill will be cast on actor's target player corpse (like Resurrection).
     */
    DEAD_PLAYER,

    /**
     * Skill will be cast on the actor himself. AoE skills around caster also have this target type
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
