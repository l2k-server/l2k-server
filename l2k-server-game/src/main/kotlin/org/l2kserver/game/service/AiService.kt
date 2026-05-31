package org.l2kserver.game.service

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.asMutable
import org.l2kserver.game.handler.dto.ChatTab
import org.l2kserver.game.handler.dto.response.ChatMessageResponse
import org.l2kserver.game.model.actor.NpcInstanceImpl
import org.l2kserver.game.model.actor.npc.ai.AiIntents
import org.l2kserver.game.model.actor.npc.ai.AttackIntent
import org.l2kserver.game.model.actor.npc.ai.MoveIntent
import org.l2kserver.game.model.actor.npc.ai.SayIntent
import org.l2kserver.game.model.actor.npc.ai.WaitIntent
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class AiService(
    override val gameObjectRepository: GameObjectRepository,

    private val moveService: MoveService,
    private val combatService: CombatService,
    private val asyncTaskService: AsyncTaskService
) : AbstractService() {
    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchRepeated("AI_JOB", 1000) {
        gameObjectRepository.findAllNpc().forEach { npc ->
            if (!npc.isDead()) runCatching { launchOnIdleAction(npc) }
                .onFailure { log.error(it) { "An error occurred when handling $npc's ai" } }
        }

        //TODO Idle actions should be performed less frequently, but what if the npc is fighting?
    }

    private suspend fun launchOnIdleAction(npc: NpcInstanceImpl) {
        val intents = npc.onIdle()
        if (!asyncTaskService.hasActionByActorId(npc.id) && !intents.isNullOrEmpty()) {
            asyncTaskService.launchAction(npc.id) { performIntendedActions(intents, npc) }
        }
    }

    private suspend fun performIntendedActions(intents: AiIntents, npc: NpcInstanceImpl) = intents.forEach { intent ->
        if (!currentCoroutineContext().isActive) return@forEach
        when (intent) {
            is WaitIntent -> delay(intent.waitTimeMillis)
            is SayIntent -> broadcastAround(npc.position) {
                ChatMessageResponse(
                    speakerId = npc.id,
                    chatTab = ChatTab.GENERAL,
                    speakerName = npc.name,
                    message = intent.message
                )
            }
            is MoveIntent -> moveService.move(npc, intent.position)
            is AttackIntent -> combatService.attack(npc, intent.target.asMutable())
        }
    }

}
