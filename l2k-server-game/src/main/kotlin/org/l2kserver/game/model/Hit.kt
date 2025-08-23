package org.l2kserver.game.model

/**
 * Data class representing single target hit
 *
 * @param targetId ID of game object that was hit
 * @param damage How many damage points has the attack dealt
 * @param usedSoulshot is soulshot used during this attack
 * @param isCritical is this attack critical
 * @param isBlocked is this attack blocked by shield
 * @param isAvoided is this attack missed
 */
data class Hit(
    val targetId: Int,
    val damage: Int = 0,

    val usedSoulshot: Boolean = false,
    val isCritical: Boolean = false,
    val isBlocked: Boolean = false,
    val isAvoided: Boolean = false
)
