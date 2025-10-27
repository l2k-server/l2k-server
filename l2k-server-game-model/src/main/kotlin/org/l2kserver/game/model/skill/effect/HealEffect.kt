package org.l2kserver.game.model.skill.effect

/**
 * Healing smb skill effect
 *
 * @property value How many HP is restored
 */
data class HealEffect(
    val targetId: Int,
    val value: Int
): Effect

fun Effects.heal(targetId: Int, value: Int) = this.add(HealEffect(targetId, value))
