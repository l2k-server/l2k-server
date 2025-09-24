package org.l2kserver.game.model.skill.action.effect

sealed interface Effect

/**
 * Data class representing single target hit
 *
 * @property damage How many damage points has the attack dealt
 * @property isCritical is this attack critical
 * @property isBlocked is this attack blocked by shield
 * @property isAvoided is this attack avoided
 * @property isMagicCritical is this attack a magical crit
 * @property isHalfSuccessful is this attack half successful
 * @property isFailed is this attack failed
 */
data class DamageEffect(
    val damage: Int = 0,
    val isCritical: Boolean = false,
    val isBlocked: Boolean = false,
    val isAvoided: Boolean = false,
    val isMagicCritical: Boolean = false,
    val isHalfSuccessful: Boolean = false,
    val isFailed: Boolean = false
): Effect
