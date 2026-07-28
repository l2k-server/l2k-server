package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.stereotype.Service
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

@Service
class IntentionExecutorService(
    private val asyncTaskService: AsyncTaskService,
    private val moveService: MoveService,
    private val combatService: CombatService,
    private val skillService: SkillService,
    private val itemService: ItemService,
    private val actionService: ActionService
) {

    private val log = logger()

    /** Storage for listeners of actor intentions. Key - actor ID, value - listening job */
    private val intentionListeners = ConcurrentHashMap<Int, Job>()

    fun launchIntentionQueueListener(actor: MutableActorInstance) {
        intentionListeners[actor.id]?.let {
            log.warn { "There is already launched listener for $actor - stopping it" }
            runBlocking { it.cancelAndJoin() }
        }

        var context = Dispatchers.Default + SupervisorJob() + MDCContext(emptyMap())
        sessionContextOf(actor.id)?.let { context += it }

        val listener = CoroutineScope(context).launch {
            actor.intentionQueue.onNext { intention ->
                log.debug { "Start handling '$intention' of '$actor'" }
                asyncTaskService.cancelActionByActorId(actor.id)

                val job = when (intention) {
                    is Intention.Move -> asyncTaskService.launchAction(actor) {
                        moveService.executeMoving(actor, intention)
                        actor.intentionQueue.shift()
                    }

                    is Intention.Attack -> asyncTaskService.launchAction(actor) {
                        combatService.attack(actor, intention.target)
                        //Enqueue further attacks if target is still alive,
                        // action is not canceled and there are no new actions
                        if (currentCoroutineContext().isActive
                            && !actor.intentionQueue.hasFurtherActions()
                            && !intention.target.isDead()
                        ) actor.intentionQueue.enqueue(
                            Intention.Move(
                                intention.target, requiredDistance = actor.stats.attackRange
                            ),
                            intention
                            // There is no need to shift queue here - it is already empty after adding Move
                        )
                    }

                    is Intention.Cast -> asyncTaskService.launchAction(actor) {
                        skillService.executeCasting(actor, intention)
                        actor.intentionQueue.shift()
                    }
                    //PickUp and Interact are momentary actions, there is no need to launch them asynchronously
                    is Intention.PickUp -> {
                        (actor as? PlayerCharacterInstanceImpl)?.let {
                            itemService.pickUp(actor, intention.item)
                            actor.intentionQueue.shift()
                        }
                        null
                    }

                    is Intention.Interact -> {
                        (actor as? PlayerCharacterInstanceImpl)?.let {
                            actionService.interact(it, intention.target)
                            actor.intentionQueue.shift()
                        }
                        null
                    }

                    null -> null
                }

                job?.invokeOnCompletion { e ->
                    if (e is CancellationException)
                        log.debug { "Intention '$intention' execution of '$actor' was cancelled" }
                    else if (e != null)
                        log.error(e) { "An error occurred during execution of $intention" }
                    else
                        log.debug { "Executed intention $intention" }
                }
            }
        }
        listener.invokeOnCompletion {
            log.debug { "Intention listener of '$actor' was finished${it?.let { " due to error $it" }}" }
            intentionListeners.remove(actor.id)
        }
        intentionListeners[actor.id] = listener
        log.debug { "Launched new intention queue listener for $actor" }
    }

    fun disableIntentionQueueListener(actorId: Int) = intentionListeners[actorId]?.cancel()

    @PreDestroy
    fun cancelAll() = intentionListeners.forEach { (_, it) -> it.cancel() }

}
