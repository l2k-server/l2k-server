package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.Spiritshot
import kotlin.math.roundToInt

/**
 * Healing smb skill effect
 *
 * @property value How many HP is restored
 */
data class HealEffect(
    override val targetId: Int,
    val value: Int
): Effect

/** Calculates healing effect */
fun HealEffect(target: ActorInstance, power: Int, usedSpiritshotType: Spiritshot.Type? = null): HealEffect {
    var healPower = power.toDouble()
    when (usedSpiritshotType) {
        Spiritshot.Type.SPIRITSHOT -> healPower *= 1.3
        Spiritshot.Type.BLESSED_SPIRITSHOT -> healPower *= 1.5
        null -> {}
    }
    //TODO Heal effectiveness buffs/debuffs (like Prayer or Touch of Death)
    return HealEffect(target.id, healPower.roundToInt())
}
