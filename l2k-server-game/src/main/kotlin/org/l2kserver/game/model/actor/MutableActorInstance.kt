package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position

/** Same as Actor, but with some mutable fields and stuff to be proceeded by server core */
sealed class MutableActorInstance: ActorInstance {
    abstract override var position: Position
    abstract override var heading: Heading
    abstract override var currentHp: Int
    abstract override var currentMp: Int
    abstract override var moveType: MoveType
    abstract override var isFighting: Boolean
    abstract override var isMoving: Boolean
    abstract override var targetId: Int?
    abstract override val targetedBy: MutableSet<ActorInstance>
}
