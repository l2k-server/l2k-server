package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.l2kserver.game.extensions.logger
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import kotlin.coroutines.coroutineContext

/**
 * This service handles async tasks, like moving, attacking, etc
 */
@Service
class AsyncTaskService {

    private val log = logger()

    /** Storage for global tasks */
    private val taskJobMap = ConcurrentHashMap<String, Job>()

    /** Storage for action jobs, performed by actors */
    private val actionJobMap = ConcurrentHashMap<Int, Job>()

    /** Cancels previous action job of actor with provided [actorId], waits for its completion and launches new [action] */
    suspend fun launchAction(actorId: Int, action: suspend CoroutineScope.() -> Unit): Job {
        actionJobMap[actorId]?.cancelAndJoin()
        val job = CoroutineScope(Dispatchers.Default + coroutineContext).launch { action() }
        job.invokeOnCompletion {
            it?.let { log.warn("Job for actor '{}' completed with error", actorId, it) }
            actionJobMap.remove(actorId)
        }

        actionJobMap[actorId] = job
        return job
    }

    /** Cancels action job of actor with provided [actorId] */
    fun cancelActionByActorId(actorId: Int) = actionJobMap.remove(actorId)
        ?.cancel("Action job for actor '$actorId' was cancelled")

    /** Checks if actor with [actorId] has launched action */
    fun hasActionByActorId(actorId: Int) = actionJobMap.containsKey(actorId)

    /**
     * Launches a task that will be called once.
     *
     * @param launchAt When the task should be called.
     * @param action Action that will be called.
     */
    fun launchOnce(launchAt: Temporal = Instant.now(), action: suspend CoroutineScope.() -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(Duration.between(Instant.now(), launchAt).toMillis())
            action()
        }
    }

    /**
     * Launches a task that will be repeated.
     *
     * @param taskName Name of starting task
     * @param interval Time between iterations
     * @param action Action that will be repeated with [interval]
     */
    fun launchRepeated(taskName: String, interval: Long, action: suspend CoroutineScope.() -> Unit) {
        taskJobMap[taskName] = CoroutineScope(Dispatchers.Default + CoroutineName(taskName)).launch {
            while (isActive) {
                val actionStartTime = System.currentTimeMillis()
                action()
                delay(interval - (System.currentTimeMillis() - actionStartTime))
            }
        }
        log.info("Started $taskName")
    }

    fun cancelTask(taskName: String) = taskJobMap[taskName]?.cancel()

    @PreDestroy
    fun shutdown() {
        taskJobMap.forEach { name, task ->
            log.info("Cancelling $name}")
            task.cancel()
        }

        actionJobMap.keys.forEach { cancelActionByActorId(it) }
    }

}
