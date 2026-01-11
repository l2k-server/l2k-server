package org.l2kserver.game.data.item.etc

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.of
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.MagicItem
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.stats.CombatStats
import java.time.Duration

private class ScrollOfGuidanceEffect(
    override val targetId: Int
): TemporalAbnormalEffect(Duration.ofMinutes(20)) {
    override val effectLevel = 1
    override val skillId = ScrollOfGuidanceSkill.id
    override val abnormalType = AbnormalType.HIT_UP

    override fun getFixedBonusStats(actor: ActorInstance) = CombatStats(
        accuracy = 4
    )
}

private data object ScrollOfGuidanceSkill: ActiveSkill() {
    override val id = 2050
    override val skillName = "Scroll of Guidance"
    override val targetType = SkillTargetType.SELF
    override val reuseDelay = 0
    override val castTime = 4_000
    override val isMagic = false
    override val consumesToStart = SkillConsumablesTemplate(item = listOf(1 of ScrollOfGuidance))
    override val usesCasterStats = false

    override fun affect(context: SkillContext) = Effects(
        ScrollOfGuidanceEffect(context.caster.id)
    )

}

data object ScrollOfGuidance: MagicItem() {
    override val id = 3926
    override val name = "Scroll of Guidance"
    override val grade = Grade.NO_GRADE
    override val weight = 120
    override val price = 1
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = true

    override val skill: ActiveSkill = ScrollOfGuidanceSkill
}
