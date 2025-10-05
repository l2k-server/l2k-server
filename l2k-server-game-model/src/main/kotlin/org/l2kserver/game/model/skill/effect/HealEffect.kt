package org.l2kserver.game.model.skill.effect

/**
 * Healing smb skill effect
 *
 * @property value How many HP is restored
 */
data class HealEffect(
    val value: Int
): Effect

fun SkillEffects.heal(value: Int) = this.add(HealEffect(value))
