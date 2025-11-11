package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import kotlin.math.roundToInt

/**
 * Healing smb skill effect
 *
 * @property value How many HP is restored
 */
data class HealEffect(
    val targetId: Int,
    val value: Int
): Effect

/** Calculates healing effect */
fun HealEffect(target: ActorInstance, power: Int, usedSpiritshotType: SpiritshotType? = null): HealEffect {
    var healPower = power.toDouble()
    when (usedSpiritshotType) {
        SpiritshotType.SPIRITSHOT -> healPower *= 1.3
        SpiritshotType.BLESSED_SPIRITSHOT -> healPower *= 1.5
        null -> {}
    }
    //TODO Heal effectiveness buffs/debuffs (like Prayer or Touch of Death)
    return HealEffect(target.id, healPower.roundToInt())
}
