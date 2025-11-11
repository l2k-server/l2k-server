@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.MagicSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate

object WindStrike: MagicSkillTemplate {
    override val id = 1177
    override val skillName = "Wind Strike"
    override val maxLevel = 5
    override val targetType = SkillTargetType.ENEMY
    override val reuseDelay = 6_000
    override val castTime = 4000
    override val castRange = 600
    override val effectRange = 1100
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(2, 2, 2, 3, 3))
    override val consumes = SkillConsumablesTemplate(mp = listOf(7, 7, 8, 11, 12))

    val power = listOf(12, 13, 15, 18, 21)
    val magicLevel = listOf(1, 4, 7, 11, 14)

    override fun affect(context: SkillContext) = Effects(
        DamageEffect.magicHit(
            caster = context.caster,
            target = context.mainTarget,
            power = power.getOrElse(context.skillLevel - 1) {
                error("Skill $skillName does not have level ${context.skillLevel}")
            },
            magicLevel = magicLevel.getOrElse(context.skillLevel - 1) {
                error("Skill $skillName does not have level ${context.skillLevel}")
            },
            usedSpiritshotType = context.usedSpiritshotType
        )
    )
}
