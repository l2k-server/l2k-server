@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.ResurrectionEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConditionFailed
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate

object Resurrection: ActiveSkill {
    override val id = 1016
    override val skillName = "Resurrection"
    override val maxLevel = 1
    override val targetType = SkillTargetType.DEAD_PLAYER
    override val reuseDelay = 120_000
    override val castTime = 6_000
    override val isMagic = true
    override val castRange = 400
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(12))
    override val consumes = SkillConsumablesTemplate(mp = listOf(47))

    override fun canBeUsed(caster: ActorInstance, target: ActorInstance): SkillConditionFailed? {
        return if ((target as? PlayerCharacterInstance)?.resurrectionIsPending == false)
            null
        else SkillConditionFailed.TargetIsPendingResurrection
    }

    override fun affect(context: SkillContext) = listOf(
        ResurrectionEffect(context.mainTarget.id, 1.0)
    )
}
