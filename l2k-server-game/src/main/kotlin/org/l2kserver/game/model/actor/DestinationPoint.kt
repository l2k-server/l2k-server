package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Position

/** Fake GameWorldObject, representing moving destination point */
data class DestinationPoint(@Volatile override var position: Position): GameWorldObject {
    override val id = 0
    override val collisionBox = CollisionBox()
}
