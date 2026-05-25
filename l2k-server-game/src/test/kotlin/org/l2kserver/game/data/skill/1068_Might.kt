@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.stats.CombatStatsMultipliers
import java.time.Duration

class MightEffect(
    override val targetId: Int,
    override val effectLevel: Int,
): TemporalAbnormalEffect(Duration.ofMinutes(2)) {
    override val skillId = Might.id
    override val abnormalType = AbnormalType.PA_UP

    private val power = listOf(1.08, 1.12, 1.15)

    override fun getCombatStatsMultipliers(actor: ActorInstance) = CombatStatsMultipliers(
        pAtk = power.getOrElse(effectLevel - 1) { error("Skill 'Might' has no level '$effectLevel'") }
    )
}

data object Might: ActiveSkill {
    override val id = 1068
    override val skillName = "Might"
    override val maxLevel = 3
    override val targetType = SkillTargetType.FRIEND
    override val reuseDelay = 6_000
    override val castTime = 4_000
    override val isMagic = true
    override val castRange = 400
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(2, 4, 7))
    override val consumes = SkillConsumablesTemplate(mp = listOf(8, 16, 28))

    override fun affect(context: SkillContext) = listOf(
        MightEffect(
            targetId = context.mainTarget.id,
            effectLevel = context.skillLevel
        )
    )
}
