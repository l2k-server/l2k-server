package org.l2kserver.game.model.skill

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.template.ActiveSkill
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Contains skill cooldowns for item skills.
 * Key - pair of (characterId, skillId), value - next usage available time millis
 * This ensures each character has individual cooldowns for item skills
 */
private val itemSkillCooldowns = ConcurrentHashMap<Pair<Int, Int>, Instant>()

/**
 * Skill from item (scroll, potion, etc.), that can be cast
 *
 * @property template Skill template
 * @property characterId ID of character who uses item
 * @property skillLevel Skill level (usually 1 for item skills)
 */
class ItemSkillInstanceImpl(
    private val template: ActiveSkill,
    private val characterId: Int,
): ActiveSkillInstance {
    override val skillId = template.id
    override val skillName = template.skillName
    override val isMagic = template.isMagic
    override val targetType = template.targetType
    override val skillLevel = 1

    override val reuseDelay: Int = template.reuseDelay
    override val castTime = template.castTime
    override val repriseTime = template.repriseTime
    override val castRange = template.castRange
    override val effectRange = template.effectRange
    override val requires = template.requires
    override val consumesToStart = template.consumesToStart?.getByLevel(skillLevel)
    override val consumes = template.consumes?.getByLevel(skillLevel)
    override val overhitPossible = template.overhitPossible
    override val forcedUsageAllowed = template.forcedUsageAllowed
    override val usesCasterStats = template.usesCasterStats

    private val cooldownKey = characterId to skillId

    override var nextUsageTime: Instant
        get() = itemSkillCooldowns[cooldownKey] ?: Instant.MIN
        set(value) {
            itemSkillCooldowns[cooldownKey] = value
        }

    override fun canBeUsed(caster: ActorInstance, target: ActorInstance) = template.canBeUsed(caster, target)
    override fun affect(context: SkillContext) = template.affect(context)

    override fun toString() = "ItemSkill(id=$skillId name=$skillName level=$skillLevel characterId=$characterId)"
}
