package org.l2kserver.game.model.actor

import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position

/** Same as Actor, but with some mutable fields to be proceeded by server core */
interface MutableActorInstance: ActorInstance {
    override var position: Position
    override var heading: Heading
    override var currentHp: Int
    override var currentMp: Int
    override var moveType: MoveType
    override var isFighting: Boolean
    override var isMoving: Boolean
    override var targetId: Int?
    override val targetedBy: MutableSet<ActorInstance>
}
