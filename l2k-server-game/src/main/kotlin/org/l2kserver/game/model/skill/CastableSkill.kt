package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.instance.CastableSkillInstance
import org.l2kserver.game.model.skill.instance.MagicSkillInstance
import org.l2kserver.game.model.skill.instance.SkillConsumables
import org.l2kserver.game.model.skill.template.ActiveSkillTemplate
import org.l2kserver.game.model.skill.template.CastableSkillTemplate
import org.l2kserver.game.model.skill.template.MagicSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
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
sealed class CastableSkill(
    private val entity: SkillEntity,
    private val template:  CastableSkillTemplate
) : CastableSkillInstance {
    companion object;

    private val skillEntityId = entity.id.value

    override val skillId = entity.skillId
    override val skillName = template.skillName
    override val skillLevel by entity::skillLevel
    override val targetType = template.targetType

    override val reuseDelay: Int = template.reuseDelay
    override val castTime = template.castTime
    override val repriseTime = template.repriseTime
    override val castRange = template.castRange
    override val effectRange = template.effectRange
    override val requires = template.requires
    override val consumesToStart: SkillConsumables? get() = template.consumesToStart?.toSkillConsumables(skillLevel)
    override val consumes: SkillConsumables? get() = template.consumes?.toSkillConsumables(skillLevel)
    override val overhitPossible = template.overhitPossible
    override val forcedUsageAllowed = template.forcedUsageAllowed

    override var nextUsageTime: Instant
        get() = cooldowns[skillEntityId] ?: Instant.MIN
        set(value) {
            cooldowns[skillEntityId] = value
        }

    override fun affect(context: SkillContext) = template.affect(context)

    override fun toString() = "ActiveSkill(id=$skillId name=$skillName level=$skillLevel)"
}

class ActiveSkill(
    entity: SkillEntity, template: ActiveSkillTemplate
): ActiveSkillInstance, CastableSkill(entity, template)

class MagicSkill(
    entity: SkillEntity, template: MagicSkillTemplate
): MagicSkillInstance, CastableSkill(entity, template)

private fun SkillConsumablesTemplate.toSkillConsumables(skillLevel: Int) = SkillConsumables(
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
