package org.l2kserver.game.model.actor

import kotlinx.coroutines.flow.MutableStateFlow
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance

/**
 * A high-level "what the actor wants to do next" model (e.g. movement, combat, skills).
 * The queueing rules are handled by [IntentionQueue].
 */
sealed interface Intention {

    /**
     * Intention to move towards [destination] until being within [requiredDistance].
     *
     * **Note**: When [Move] is the current intention, [IntentionQueue] may *mutate* the existing instance (retargeting)
     * rather than replace it;
     * the movement executor should observe [destination]/[requiredDistance] changes while running.
     */
    data class Move(
        @Volatile var destination: GameWorldObject,
        @Volatile var requiredDistance: Int = 0
    ): Intention

    /**
     * Intention to perform **single** auto-attack on [target]
     */
    data class Attack(
        val target: MutableActorInstance
    ): Intention

    /**
     * Intention to cast [skill] at [target] with the provided behavioral flags:
     * - [forced]: bypass some target checks (f.e. cast healing on enemy target)
     * - [holdPosition]: do not move towards target if it is not enough close
     */
    data class Cast(
        val skill: ActiveSkillInstance,
        val target: MutableActorInstance,
        val forced: Boolean,
        val holdPosition: Boolean
    ): Intention

    /**
     * Intention to pick up [item].
     */
    data class PickUp(
        val item: ScatteredItem
    ): Intention

    /**
     * Intention to interact (open chat window, private store, etc.) with [target].
     */
    data class Interact(
        val target: ActorInstance
    ): Intention

}

/**
 * A per-actor intention queue with simple conflict-resolution rules. (thread-safe). It
 * - **Stores** the current intention (head) and a FIFO queue of further intentions.
 * - **Resolves conflicts** when new intentions arrive (replace current, mutate current, clear tail, or enqueue).
 * - **Publishes changes** of the current intention (see [onNext]).
 *
 * A single consumer (typically an "intention executor" service) should:
 * - subscribe using [onNext] (usually once per actor), and
 * - run/cancel actual coroutines/jobs for the actor based on the current intention value, and
 * - call [shift] when the current intention is **completed** and the next intention may start.
 *
 * ### Enqueue rules - how new intentions are handled depending on the current intention:
 * - **Move**:
 *   - if current is `null` or `Attack` → becomes the new current intention
 *   - if current is `Move` → clears the tail and mutates current move's destination/distance
 *   (collectors of [onNext] will **not** receive a new emission (the reference stays the same))
 *   - otherwise → enqueues to the tail
 * - **Cast**:
 *   - if current is `null` or `Attack` → becomes the new current intention
 *   - if current is `Move` → clears the tail and enqueues cast (cast after reaching destination)
 *   - otherwise → enqueues to the tail
 * - **Default** (Attack, PickUp, Interact, etc.):
 *   - if current is `null` → becomes the new current intention
 *   - if current is `Move` → clears the tail and enqueues default (default after reaching destination)
 *   - otherwise → enqueues to the tail
 */
class IntentionQueue {
    private val currentIntentionFlow = MutableStateFlow<Intention?>(null)

    /**
     * Current intention (head of the queue).
     *
     * Prefer [onNext] for reacting to changes. This property is a snapshot.
     */
    val current: Intention? get() = currentIntentionFlow.value

    /**
     * Contains all the next intentions.
     * [ArrayDeque] itself is not thread-safe; all modifications must stay under `synchronized(this)`.
     */
    private val queue = ArrayDeque<Intention>()

    /**
     * Subscribes to current intention changes.
     */
    suspend fun onNext(block: suspend (Intention?) -> Unit): Unit = currentIntentionFlow.collect { block(it) }

    /**
     * Advances to the next intention from the tail queue (FIFO).
     * If the tail is empty, the current intention becomes `null` (idle).
     */
    fun shift() = synchronized(this) {
        currentIntentionFlow.value = queue.removeFirstOrNull()
    }

    /**
     * Cancels the whole intention chain: clears the tail queue and sets [current] to `null`.
     */
    fun cancel() = synchronized(this) {
        queue.clear()
        currentIntentionFlow.value = null
    }

    /**
     * Clears all queued intentions *after* the current one.
     */
    fun clearFurtherActions() = synchronized(this) {
        queue.clear()
    }

    /**
     * Returns true if the given [intention] is the current one or present in the tail queue.
     *
     * This is a convenience check and may be stale immediately after returning in a concurrent environment.
     */
    fun contains(intention: Intention) = intention == current || queue.contains(intention)

    /**
     * Enqueues one or more intentions applying the queue conflict-resolution rules.
     * See the class-level documentation for the detailed rule set.
     */
    fun enqueue(vararg intentions: Intention) = synchronized(this) {
        for (it in intentions) {
            when (it) {
                is Intention.Move -> enqueueMove(it)
                is Intention.Cast -> enqueueCast(it)
                else -> enqueueDefault(it)
            }
        }
    }

    private fun enqueueMove(intention: Intention.Move) = when (current) {
        null, is Intention.Attack -> currentIntentionFlow.value = intention
        is Intention.Move -> {
            //Clear further intentions
            queue.clear()
            //Change moving direction
            (current as? Intention.Move)?.let {
                it.destination = intention.destination
                it.requiredDistance = intention.requiredDistance
            }
        }
        else -> queue.addLast(intention)
    }

    private fun enqueueCast(intention: Intention.Cast) {
        when (current) {
            null, is Intention.Attack -> currentIntentionFlow.value = intention
            is Intention.Move -> {
                queue.clear()
                queue.addLast(intention)
            }
            else -> queue.addLast(intention)
        }
    }

    private fun enqueueDefault(intention: Intention) = when (current) {
        null -> currentIntentionFlow.value = intention
        is Intention.Move -> {
            queue.clear()
            queue.addLast(intention)
        }
        else -> queue.addLast(intention)
    }
}
