package org.l2kserver.game.model.actor

import org.l2kserver.game.domain.TemporalEffects
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position

/** Same as [ActorInstance], but with some mutable fields and stuff to be proceeded by server core */
sealed class MutableActorInstance: ActorInstance {
    abstract override var position: Position
    abstract override var heading: Heading
    abstract override var currentHp: Int
    abstract override var currentMp: Int
    abstract override var moveType: MoveType
    abstract override var isFighting: Boolean
    abstract override var targetId: Int?
    abstract override val targetedBy: MutableSet<ActorInstance>
    abstract override val temporalEffects: TemporalEffects

    val intentionQueue = IntentionQueue()
    @Volatile var knownGameWorldObjects: Set<GameWorldObject> = emptySet()

    override val isMoving get() = intentionQueue.current is Intention.Move
    val movementDestination get() = (intentionQueue.current as? Intention.Move)?.destination?.position
}
