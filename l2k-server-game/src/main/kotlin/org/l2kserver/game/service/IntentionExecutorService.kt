package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.network.session.sessionContextOf
import org.springframework.stereotype.Service
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
            log.warn("There is already launched listener for {} - stopping it", actor)
            runBlocking { it.cancelAndJoin() }
        }

        var context = Dispatchers.Default + SupervisorJob()
        sessionContextOf(actor.id)?.let { context += it }

        val listener = CoroutineScope(context).launch {
            actor.intentionQueue.onNext { intention ->
                log.debug("Start handling '{}' of '{}'", intention, actor)
                asyncTaskService.cancelActionByActorId(actor.id)

                val job = when (intention) {
                    is Intention.Move -> asyncTaskService.launchAction(actor.id) {
                        moveService.executeMoving(actor, intention)
                        actor.intentionQueue.shift()
                    }
                    is Intention.Attack -> asyncTaskService.launchAction(actor.id) {
                        combatService.attack(actor, intention.target)
                        //Enqueue further attacks if target is still alive and action is not canceled
                        if (isActive && !intention.target.isDead()) actor.intentionQueue.enqueue(
                            Intention.Move(
                                intention.target, requiredDistance = actor.stats.attackRange
                            ),
                            intention
                        )
                        // Move intention must cancel attack, so there is no need to shift queue here -
                        // it is already empty after adding Move
                    }
                    is Intention.Cast -> asyncTaskService.launchAction(actor.id) {
                        skillService.executeCasting(actor, intention)
                        actor.intentionQueue.shift()
                    }
                    //PickUp and Interaction are momentary actions, there is no need to launch them asynchronously
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
                    if (e != null) log.error("An error occurred during execution of {}", intention, e)
                    else log.debug("Executed intention {}", intention)
                }
            }
        }
        listener.invokeOnCompletion { intentionListeners.remove(actor.id) }
        intentionListeners[actor.id] = listener
        log.debug("Launched new intention queue listener for {}", actor)
    }

    fun disableIntentionQueueListener(actorId: Int) = intentionListeners[actorId]?.cancel()

    @PreDestroy
    fun cancelAll() = intentionListeners.forEach { (_, it) -> it.cancel() }

}
