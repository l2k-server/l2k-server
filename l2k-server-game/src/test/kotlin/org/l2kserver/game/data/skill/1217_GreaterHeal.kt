@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.model.skill.effect.EffectOnTimeAbnormalEffect
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.effect.HealEffect
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.MagicSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import java.time.Duration
import kotlin.math.roundToInt

class GreaterHealEffect(
    override val targetId: Int,
    override val effectLevel: Int
) : EffectOnTimeAbnormalEffect(Duration.ofSeconds(15)) {
    override val skillId: Int = GreaterHeal.id
    override val abnormalType = AbnormalType.HP_RECOVER

    private val recoverPower = listOf(15, 15, 15)

    override fun effects(context: SkillContext) = Effects(
        HealEffect(
            target = context.mainTarget,
            power = recoverPower.getOrElse(context.skillLevel - 1) {
                error("Cannot find heal power of 'Greater Heal' by skill level '${context.skillLevel}'")
            }
        )
    )
}

object GreaterHeal: MagicSkillTemplate() {
    override val id = 1217
    override val skillName = "Greater Heal"
    override val maxLevel = 3
    override val targetType = SkillTargetType.FRIEND
    override val reuseDelay = 6_000
    override val castTime = 4_000
    override val castRange = 400
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(2, 4, 7))
    override val consumes = SkillConsumablesTemplate(mp = listOf(8, 16, 28))

    private val healPower = listOf(204, 212, 219)
    private val recoverEffectLevel = listOf(2, 2, 2)

    override fun affect(context: SkillContext) = effects {
        val target = context.mainTarget
        val actionLevel = context.skillLevel
        val usedSpiritshotType = context.usedSpiritshotType

        val healPower = healPower.getOrNull(actionLevel - 1)?.toDouble()
            ?: error("Cannot find heal power of 'Greater Heal' by skill level '$actionLevel'")

        val recoverEffectLevel = recoverEffectLevel.getOrNull(actionLevel - 1)
            ?: error("Cannot find hp recover effect level of 'Greater Heal' by skill level '$actionLevel'")

        add(HealEffect(target, healPower.roundToInt(), usedSpiritshotType))
        add(GreaterHealEffect(target.id, recoverEffectLevel))
    }
}
