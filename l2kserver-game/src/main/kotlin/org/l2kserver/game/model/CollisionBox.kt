package org.l2kserver.game.model

/**
 * Represents characters hitbox. In L2 hitbox is cylindrical
 */
data class CollisionBox(
    val radius: Double,
    val height: Double
)
