package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.Spiritshot
import kotlin.math.roundToInt

private const val SPIRITSHOT_HEAL_MULTIPLIER = 1.3
private const val BLESSED_SPIRITSHOT_HEAL_MULTIPLIER = 1.5
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
        Spiritshot.Type.SPIRITSHOT -> healPower *= SPIRITSHOT_HEAL_MULTIPLIER
        Spiritshot.Type.BLESSED_SPIRITSHOT -> healPower *= BLESSED_SPIRITSHOT_HEAL_MULTIPLIER
        null -> {}
    }
    //TODO Heal effectiveness buffs/debuffs (like Prayer or Touch of Death)
    return HealEffect(target.id, healPower.roundToInt())
}
