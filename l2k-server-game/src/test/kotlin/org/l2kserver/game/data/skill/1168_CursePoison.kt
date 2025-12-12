package org.l2kserver.game.data.skill

import org.l2kserver.game.model.skill.context.SkillContext
import org.l2kserver.game.model.skill.effect.AbnormalType
import org.l2kserver.game.model.skill.effect.DamageEffect
import org.l2kserver.game.model.skill.effect.EffectOnTimeAbnormalEffect
import org.l2kserver.game.model.skill.effect.Effects
import org.l2kserver.game.model.skill.effect.AbnormalVisualEffect
import org.l2kserver.game.model.skill.effect.Debuff
import org.l2kserver.game.model.skill.effect.ResistedEffect
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.instance.SkillTargetType
import org.l2kserver.game.model.skill.template.MagicSkillTemplate
import org.l2kserver.game.model.skill.template.SkillConsumablesTemplate
import org.l2kserver.game.model.stats.BasicStat
import org.l2kserver.game.model.stats.CON
import org.l2kserver.game.model.utils.calculateDebuffSuccess
import java.time.Duration

class PoisonEffect(
    override val targetId: Int,
    override val magicLevel: Int
) : EffectOnTimeAbnormalEffect(duration = Duration.ofSeconds(30)), Debuff {
    override val skillId = CursePoison.id
    override val abnormalType = AbnormalType.POISON
    override val abnormalVisualEffect = AbnormalVisualEffect.POISON
    override val effectLevel get() = listOf(1, 3, 4)[magicLevel - 1]

    override val basicProperty: BasicStat = CON
    override val levelBonusRate = 1.0
    override val activateRate = 70

    val power = listOf(8, 18, 24)

    override fun effects(context: SkillContext) = Effects(
        DamageEffect(
            targetId = context.mainTarget.id,
            damage = power.getOrElse(context.skillLevel - 1) {
                error("Skill 'Curse: Poison' has no level '${magicLevel}'")
            },
            isDeathly = false
        )
    )

}

object CursePoison: MagicSkillTemplate() {
    override val id = 1168
    override val skillName = "Curse: Poison"
    override val maxLevel = 3
    override val targetType = SkillTargetType.ENEMY
    override val reuseDelay = 12_000
    override val castTime = 4_000
    override val castRange = 600
    override val consumesToStart = SkillConsumablesTemplate(mp = listOf(2, 4, 6))
    override val consumes = SkillConsumablesTemplate(mp = listOf(8, 16, 21))

    override fun affect(context: SkillContext) = effects {
        val effect = PoisonEffect(targetId = context.mainTarget.id, magicLevel = context.skillLevel)
        val isSuccessful = calculateDebuffSuccess(
            caster = context.caster,
            target = context.mainTarget,
            debuff = effect,
            isMagic = true,
            usedSpiritshotType = context.usedSpiritshotType
        )

        if (isSuccessful) add(effect)
        else add(ResistedEffect(context.mainTarget.id, id))
    }

}
