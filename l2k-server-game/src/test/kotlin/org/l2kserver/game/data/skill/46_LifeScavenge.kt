@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.effect.HealEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.MagicSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import kotlin.math.roundToInt

object LifeScavenge: MagicSkillTemplate() {
    override val id = 46
    override val skillName = "Life Scavenge"
    override val maxLevel = 15
    override val targetType = SkillTargetType.DEAD_NPC
    override val reuseDelay = 20_000
    override val castTime = 1500
    override val castRange = 400
    override val effectRange = 900
    override val consumesToStart = SkillConsumablesTemplate(
        mp = listOf(7, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 13, 14, 14)
    )
    override val consumes = SkillConsumablesTemplate(
        mp = listOf(28, 30, 33, 35, 38, 40, 43, 44, 46, 48, 49, 51, 52, 53, 55)
    )

    val power = listOf(105, 113, 122, 131, 140, 150, 159, 169, 180, 190, 201, 211, 222, 232, 243)

    //TODO RND Corpse Drain works another way
    override fun affect(context: SkillContext) = effects {
        var restoredHp = (power.getOrNull(context.skillLevel - 1) ?: 0).toDouble()

        when (context.usedSpiritshotType) {
            SpiritshotType.SPIRITSHOT -> restoredHp *= 1.3
            SpiritshotType.BLESSED_SPIRITSHOT -> restoredHp *= 1.5
            null -> {}
        }

        //TODO Heal effectiveness buffs/debuffs (like Prayer or Touch of Death)
        add(HealEffect(context.caster, restoredHp.roundToInt()))
    }
}
