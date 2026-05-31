package org.l2kserver.game.data.item.scroll

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.item.of
import org.l2kserver.game.model.item.MagicItem
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.ResurrectionEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.SkillConditionFailed
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate

private data object ScrollOfResurrectionSkill: ActiveSkill {
    override val id = 2014
    override val skillName = "s_scroll_of_resurrection"
    override val targetType = SkillTargetType.DEAD_PLAYER
    override val reuseDelay = 0
    override val castTime = 15_000
    override val isMagic = true
    override val castRange = 400
    override val consumesToStart = SkillConsumablesTemplate(item = 1 of ScrollOfResurrection)
    override val usesCasterStats = false

    override fun canBeUsed(caster: ActorInstance, target: ActorInstance): SkillConditionFailed? {
        return if ((target as? PlayerCharacterInstance)?.resurrectionIsPending == false)
            null
        else SkillConditionFailed.TargetIsPendingResurrection
    }

    override fun affect(context: SkillContext) = listOf(ResurrectionEffect(context.mainTarget.id))
}

data object ScrollOfResurrection: MagicItem() {
    override val id = 737
    override val name = "Scroll of Resurrection"
    override val weight = 120
    override val price = 400
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = true
    override val skill: ActiveSkill = ScrollOfResurrectionSkill
}
