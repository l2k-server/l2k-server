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

class DefenseAuraEffect(
    override val targetId: Int,
    override val effectLevel: Int
): TemporalAbnormalEffect(Duration.ofMinutes(2)) {
    override val skillId = DefenseAura.id
    override val abnormalType = AbnormalType.PD_UP

    private val power = listOf(1.08, 1.12)

    override fun getCombatStatsMultipliers(actor: ActorInstance) = CombatStatsMultipliers(
        pDef = power.getOrElse(effectLevel - 1) {
            error("Skill 'Defence Aura' has no level '${effectLevel}'")
        }
    )
}

data object DefenseAura: ActiveSkill {
    override val id = 91
    override val skillName = "Defense Aura"
    override val maxLevel = 2
    override val targetType = SkillTargetType.SELF
    override val reuseDelay = 6_000
    override val castTime = 4_000
    override val isMagic = true
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(1, 2))
    override val consumes = SkillConsumablesTemplate(mp = listOf(4, 8))

    override fun affect(context: SkillContext) = listOf(
        DefenseAuraEffect(
            context.mainTarget.id,
            context.skillLevel
        )
    )
}
