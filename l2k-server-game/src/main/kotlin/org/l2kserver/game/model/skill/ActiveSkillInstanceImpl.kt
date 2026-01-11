package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillConsumables
import org.l2kserver.game.model.skill.template.ActiveSkill
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Contains skill cooldowns. Key - skill entity ID, value - next usage available time millis */
private val cooldowns = ConcurrentHashMap<Int, Instant>()

/**
 * Skill, that can be cast
 *
 * @property skillId Skill identifier
 * @property skillName Skill name (eng)
 * @property skillLevel Level of skill
 * @property reuseDelay Base cooldown of this skill
 * @property castTime Base casting time of this skill
 * @property repriseTime Time to return to the starting position after skill casting
 * @property castRange Range to target to cast this skill, or radius for mass skill
 * @property effectRange TODO
 * @property requires Requirements to use this skill
 * @property consumes Skill consumables - mp, items, etc.
 */
class ActiveSkillInstanceImpl(
    private val entity: SkillEntity,
    private val template: ActiveSkill,
): ActiveSkillInstance {
    private val skillEntityId = entity.id.value

    override val skillId = entity.skillId
    override val skillName = template.skillName
    override val skillLevel by entity::skillLevel
    override val isMagic = template.isMagic
    override val targetType = template.targetType

    override val reuseDelay: Int = template.reuseDelay
    override val castTime = template.castTime
    override val repriseTime = template.repriseTime
    override val castRange = template.castRange
    override val effectRange = template.effectRange
    override val requires = template.requires
    override val consumesToStart: SkillConsumables? get() = template.consumesToStart?.getByLevel(skillLevel)
    override val consumes: SkillConsumables? get() = template.consumes?.getByLevel(skillLevel)
    override val overhitPossible = template.overhitPossible
    override val forcedUsageAllowed = template.forcedUsageAllowed
    override val usesCasterStats = template.usesCasterStats

    override var nextUsageTime: Instant
        get() = cooldowns[skillEntityId] ?: Instant.MIN
        set(value) {
            cooldowns[skillEntityId] = value
        }

    override fun affect(context: SkillContext) = template.affect(context)

    override fun toString() = "ActiveSkill(id=$skillId name=$skillName level=$skillLevel)"
}
