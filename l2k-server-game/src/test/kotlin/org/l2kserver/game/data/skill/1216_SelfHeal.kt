@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.effect.HealEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate

data object SelfHeal: ActiveSkill() {
    override val id = 1216
    override val skillName = "Self Heal"
    override val maxLevel = 1
    override val targetType = SkillTargetType.SELF
    override val reuseDelay = 10_000
    override val castTime = 5000
    override val isMagic = true
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(2))
    override val consumes = SkillConsumablesTemplate(mp = listOf(7))

    override fun affect(context: SkillContext) = Effects(
        HealEffect(context.caster, power = 42, context.usedSpiritshotType)
    )
}
