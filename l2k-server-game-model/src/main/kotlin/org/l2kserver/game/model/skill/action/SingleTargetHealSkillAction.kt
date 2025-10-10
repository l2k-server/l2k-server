package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.effect.effects
import org.l2kserver.game.model.skill.effect.heal
import kotlin.math.roundToInt

/**
 * Heals single target
 *
 * @property power Array of effect power per effect level (0 based)
 */
class SingleTargetHealSkillAction(
    val power: List<Int>
): SingleTargetMagicSkillAction {

    override fun apply(
        target: ActorInstance, caster: ActorInstance, actionLevel: Int, usedSpiritshotType: SpiritshotType?
    ) = effects {
        var restoredHp = (power.getOrNull(actionLevel - 1) ?: 0).toDouble()

        when (usedSpiritshotType) {
            SpiritshotType.SPIRITSHOT -> restoredHp *= 1.3
            SpiritshotType.BLESSED_SPIRITSHOT -> restoredHp *= 1.5
            null -> {}
        }

        //TODO Heal effectiveness buffs/debuffs (like Prayer or Touch of Death)

        heal(target.id, restoredHp.roundToInt())
    }

}
