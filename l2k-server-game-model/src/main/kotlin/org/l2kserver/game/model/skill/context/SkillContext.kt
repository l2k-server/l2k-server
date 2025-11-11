package org.l2kserver.game.model.skill.context

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType

/**
 * Skill usage context
 *
 * @property caster Actor, who casts the skill
 * @property mainTarget Actor, who is target of casting skill
 * @property skillLevel Level of used skill
 * @property additionalEnemyTargets enemies,
 * calculated using [org.l2kserver.game.model.skill.instance.CastableSkillInstance.targetType]
 * and [org.l2kserver.game.model.skill.instance.CastableSkillInstance.effectRange]
 * @property additionalFriendlyTargets friendly actors,
 * calculated using [org.l2kserver.game.model.skill.instance.CastableSkillInstance.targetType]
 * and [org.l2kserver.game.model.skill.instance.CastableSkillInstance.effectRange]
 * @property usedSoulshot If soulshot was charged before casting skill
 * @property usedSpiritshotType Type of spiritshot charged before casting the skill
 */
data class SkillContext(
    val caster: ActorInstance,
    val mainTarget: ActorInstance,
    val skillLevel: Int,
    val additionalEnemyTargets: Iterable<ActorInstance>,
    val additionalFriendlyTargets: Iterable<ActorInstance>,
    val usedSoulshot: Boolean = false,
    val usedSpiritshotType: SpiritshotType? = null
)
