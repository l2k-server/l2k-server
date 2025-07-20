package org.l2kserver.game.model.actor

import org.l2kserver.game.model.CollisionBox
import org.l2kserver.game.model.position.Position

/**
 * Interface for all game objects in the world - characters, monsters, dropped items etc.
 *
 * @property id Game object unique identifier
 * @property position Game object position in game world
 * @property collisionBox This game object's collision box
 */
sealed interface GameObject {
    val id: Int
    val position: Position
    val collisionBox: CollisionBox
}
