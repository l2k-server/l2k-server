package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.utils.withDelay

import java.util.concurrent.CancellationException
import kotlin.collections.set

/**
 * This service handles async tasks - character one's, like moving, attacking, etc.,
 * and global - regeneration, state updates, etc.
 */
@Service
class AsyncTaskService {

    private val log = logger()

    /** Storage for global tasks */
    private val taskJobMap = ConcurrentHashMap<String, Job>()

    /** Storage for action jobs, performed by actors */
    private val actionJobMap = ConcurrentHashMap<Int, Job>()

    /**
     * Cancels previous action job of [actor], waits for its completion and launches new [action]
     */
    suspend fun launchAction(actor: ActorInstance, action: suspend CoroutineScope.() -> Unit): Job {
        val currentJob = actionJobMap[actor.id]
        val context = Dispatchers.Default + currentCoroutineContext() + CoroutineName("Task of $actor")
        val job = CoroutineScope(context).launch {
            currentJob?.cancelAndJoin()
            action()
        }

        job.invokeOnCompletion {
            if (it != null && it !is CancellationException)
                log.warn(it) { "Job for actor '$actor.id' completed with error" }
            actionJobMap.remove(actor.id)
        }

        actionJobMap[actor.id] = job
        return job
    }

    /** Cancels action job of actor with provided [actorId] */
    suspend fun cancelActionByActorId(actorId: Int) = actionJobMap.remove(actorId)?.let {
        log.debug { "Action job for actor '$actorId' was cancelled" }
        it.cancelAndJoin()
    }

    /**
     * Launches a task that will be called once.
     *
     * @param action Action that will be called.
     */
    fun launchOnce(action: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.Default).launch {
        action()
    }

    /**
     * Launches a task that will be repeated every [millis] ms.
     *
     * @param taskName Name of starting task
     * @param millis Time between iterations
     * @param action Action that will be repeated with [millis] interval
     */
    fun launchRepeated(taskName: String, millis: Long = 100L, action: suspend CoroutineScope.() -> Unit) {
        taskJobMap[taskName] = CoroutineScope(Dispatchers.Default + CoroutineName(taskName)).launch {
            while (isActive) withDelay(millis) { action() }
        }
        log.info { "Started $taskName" }
    }

    fun cancelTask(taskName: String) = taskJobMap[taskName]?.cancel()

    @PreDestroy
    fun shutdown() = runBlocking {
        taskJobMap.forEach { (name, task) ->
            log.info { "Cancelling $name}" }
            task.cancel()
        }

        actionJobMap.keys.forEach { cancelActionByActorId(it) }
    }

}
