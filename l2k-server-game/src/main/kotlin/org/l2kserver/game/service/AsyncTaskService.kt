package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.MutableActorInstance
import org.l2kserver.game.model.extensions.forEachInstance
import org.l2kserver.game.repository.GameObjectRepository

/**
 * This service handles async tasks, like moving, attacking, etc
 */
@Service
class AsyncTaskService(
    private val gameObjectRepository: GameObjectRepository
) {

    private val log = logger()

    /** Storage for global tasks */
    private val taskJobMap = ConcurrentHashMap<String, Job>()

    /** Launches a global task */
    fun launchTask(taskName: String, action: suspend CoroutineScope.() -> Unit) {
        taskJobMap[taskName] = CoroutineScope(Dispatchers.Default + CoroutineName(taskName)).launch(block = action)
        log.info("Started $taskName")
    }

    fun cancelTask(taskName: String) = taskJobMap[taskName]?.cancel()

    @PreDestroy
    fun shutdown() {
        taskJobMap.forEach { name, task ->
            log.info("Cancelling $name}")
            task.cancel()
        }

        runBlocking { gameObjectRepository.forEachInstance<MutableActorInstance> { it.cancelAction() }}
    }

}
