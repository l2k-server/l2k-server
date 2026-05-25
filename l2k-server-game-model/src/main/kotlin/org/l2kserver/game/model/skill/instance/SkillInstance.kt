package org.l2kserver.game.model.skill.instance

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalEffect
import org.l2kserver.game.model.skill.effect.Effect
import org.l2kserver.game.model.skill.template.SkillConditionFailed
import org.l2kserver.game.model.skill.template.SkillRequirements
import java.time.Instant

/**
 * Skill instance
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillLevel Level of skill (learnt)
 */
sealed interface SkillInstance {
    val skillId: Int
    val skillName: String
    val skillLevel: Int
}

interface PassiveSkillInstance: SkillInstance {
    fun effect(actor: ActorInstance): AbnormalEffect
}

/**
 * Active skill instance
 *
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumesToStart Consumables, required to START casting skill - consumed before casting start
 * @property consumes Skill consumables - mp, items, etc.
 * @property overhitPossible Can this skill produce an over-hit
 * @property forcedUsageAllowed Can this skill be used on incorrect target (CTRL pressed)
 */
interface ActiveSkillInstance: SkillInstance {
    val targetType: SkillTargetType
    val reuseDelay: Int
    val castTime: Int
    val isMagic: Boolean
    val repriseTime: Int
    val castRange: Int
    val effectRange: Int
    val requires: SkillRequirements?
    val consumesToStart: SkillConsumables?
    val consumes: SkillConsumables?
    val overhitPossible: Boolean
    val forcedUsageAllowed: Boolean
    val usesCasterStats: Boolean
    var nextUsageTime: Instant

    fun canBeUsed(caster: ActorInstance, target: ActorInstance): SkillConditionFailed?
    fun affect(context: SkillContext): Iterable<Effect>
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
    val hp: Int = 0,
    val mp: Int = 0,
    val item: ConsumableItem? = null
)
