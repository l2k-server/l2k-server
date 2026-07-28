package org.l2kserver.game.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.asMutable
import org.l2kserver.game.extensions.model.actor.isAttacking
import org.l2kserver.game.extensions.model.actor.toFighting
import org.l2kserver.game.handler.dto.ChatTab
import org.l2kserver.game.handler.dto.response.ChatMessageResponse
import org.l2kserver.game.model.actor.Intention
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.npc.NpcState
import org.l2kserver.game.model.actor.npc.ai.AiDesire
import org.l2kserver.game.model.actor.npc.ai.NpcAi
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class AiService(
    private val asyncTaskService: AsyncTaskService,
    override val gameObjectRepository: GameObjectRepository
) : AbstractService() {

    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchRepeated("AI_JOB", 1000) {
        gameObjectRepository.findAllNpc()
            .filter { !it.isDead() }
            .forEach { npc ->
                try {
                    npc.ai?.onTick()?.forEach { performDesiredAction(npc, it) }
                }
                catch (e: Throwable) {
                    log.error(e) { "An error occurred when handling $npc's ai" }
                }
            }
    }

    private suspend fun performDesiredAction(npc: NpcInstanceImpl, desire: AiDesire) {
        log.debug { "Started handling $desire of '$npc'" }
        when (desire) {
            is AiDesire.Attack -> {
                val target = desire.target.asMutable()
                npc.state.toFighting(target)

                if (npc.isAttacking(target)) log.debug { "$npc is already attacking $target" }
                else npc.intentionQueue.enqueue(
                    Intention.Move(target, requiredDistance = npc.stats.attackRange),
                    Intention.Attack(desire.target.asMutable())
                )
            }
            is AiDesire.Move -> {
                npc.intentionQueue.enqueue(Intention.Move(desire.position))
            }
            is AiDesire.Say -> broadcastAround(npc.position) {
                ChatMessageResponse(
                    speakerId = npc.id,
                    chatTab = ChatTab.GENERAL,
                    speakerName = npc.name,
                    message = desire.message
                )
            }
        }
    }

}
