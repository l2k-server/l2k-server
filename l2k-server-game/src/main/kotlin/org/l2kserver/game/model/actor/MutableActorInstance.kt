package org.l2kserver.game.model.actor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import kotlin.coroutines.coroutineContext

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

    private var action: Job? = null
    private val actionMutex = Mutex()

    /** Cancels previous action job of actor, waits for its completion and launches [nextAction] */
    suspend fun launchAction(nextAction: suspend CoroutineScope.() -> Unit): Job = actionMutex.withLock {
        action?.cancelAndJoin()
        val job = CoroutineScope(Dispatchers.Default + coroutineContext).launch { nextAction() }
        job.invokeOnCompletion {
            it?.let {
                if (it !is CancellationException)
                    System.err.println("Job for actor '${this.id}' completed with error $it")
            }
            action = null
        }

        action = job
        return job
    }

    /** Cancels and waits for this actor's action completion */
    suspend fun cancelAction() = actionMutex.withLock {
        action?.cancelAndJoin()
        action = null
    }

    /** Returns `true` if actor is now doing something */
    fun hasAction() = action != null
}
