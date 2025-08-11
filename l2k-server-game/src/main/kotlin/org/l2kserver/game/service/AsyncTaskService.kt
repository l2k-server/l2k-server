package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineName
import org.l2kserver.game.extensions.logger
import kotlin.coroutines.coroutineContext

/**
 * This service handles async tasks, like moving, attacking, etc
 */
@Service
class AsyncTaskService {

    private val log = logger()

    /**
     * Storage for action jobs, performed by actors
     */
    private val actionJobMap = ConcurrentHashMap<Int, Job>()
    private val tasks = ConcurrentHashMap.newKeySet<Job>()

    /**
     * Cancels action job of actor with provided [actorId]
     */
    fun cancelActionByActorId(actorId: Int) = actionJobMap.remove(actorId)
        ?.cancel("Action job for actor '$actorId' was cancelled")

    /**
     * Cancels action job of actor with provided [actorId]
     */
    suspend fun cancelAndJoinActionByActorId(actorId: Int) = actionJobMap
        .remove(actorId)?.cancelAndJoin()

    fun hasActionByActorId(actorId: Int) = actionJobMap.containsKey(actorId)

    /**
     * Cancels previous action job of actor with provided [actorId], waits for its completion and launches new [action]
     */
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

    /**
     * Launches a global task
     */
    fun launchJob(taskName: String, task: suspend CoroutineScope.() -> Unit) {
        tasks.add(CoroutineScope(Dispatchers.Default + CoroutineName(taskName)).launch(block = task))
        log.info("Started $taskName")
    }

    @PreDestroy
    fun shutdown() {
        actionJobMap.keys.forEach { cancelActionByActorId(it) }

        tasks.forEach {
            log.info("Cancelling ${(it as CoroutineScope).coroutineContext[CoroutineName]!!.name}")
            it.cancel()
        }
    }

}
