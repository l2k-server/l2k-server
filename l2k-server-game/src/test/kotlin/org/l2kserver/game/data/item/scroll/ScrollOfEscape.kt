package org.l2kserver.game.data.item.scroll

import org.l2kserver.game.model.item.of
import org.l2kserver.game.model.item.MagicItem
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.EscapeEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate

private data object ScrollOfEscapeSkill: ActiveSkill {
    override val id = 2013
    override val skillName = "s_scroll_of_escape"
    override val targetType = SkillTargetType.SELF
    override val reuseDelay = 0
    override val castTime = 20_000
    override val isMagic = true
    override val consumesToStart = SkillConsumablesTemplate(item = 1 of ScrollOfEscape)
    override val usesCasterStats = false

    override fun affect(context: SkillContext) = listOf(EscapeEffect(context.caster.id))
}

data object ScrollOfEscape: MagicItem() {
    override val id = 736
    override val name = "Scroll of Escape"
    override val weight = 120
    override val price = 400
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = true
    override val skill: ActiveSkill = ScrollOfEscapeSkill
}
