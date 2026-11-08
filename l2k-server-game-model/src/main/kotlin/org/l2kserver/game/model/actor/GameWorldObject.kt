package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Position

/**
 * Interface for all game objects in the world - characters, monsters, dropped items etc.
 *
 * @property id Game worldObject unique identifier
 * @property position Game worldObject position in game world
 * @property collisionBox This game worldObject's collision box
 */
interface GameWorldObject {
    val id: Int
    val position: Position
    val collisionBox: CollisionBox
}
