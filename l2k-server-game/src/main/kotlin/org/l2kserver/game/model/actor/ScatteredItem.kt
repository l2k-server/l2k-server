package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Position

/**
 * Represents item on the ground
 *
 * @property id Scattered item identifier
 * @property position Position in game world
 * @property templateId Item template id (for example Squire's Shirt's item id is 1146)
 * @property isStackable Is this item stackable
 * @property amount Amount of items in stack
 * @property enchantLevel Item enchant level
 */
data class ScatteredItem(
    override val id: Int,
    override val position: Position,
    val templateId: Int,
    val isStackable: Boolean,
    val amount: Int,
    val enchantLevel: Int
): GameWorldObject {
    override val collisionBox = CollisionBox()
}
