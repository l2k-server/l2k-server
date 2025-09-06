package org.l2kserver.game.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.l2kserver.game.model.extensions.forEachInstance
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.actor.asMutable
import org.l2kserver.game.handler.dto.response.ChatMessageResponse
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.model.actor.npc.ai.AiIntents
import org.l2kserver.game.model.actor.npc.ai.AttackIntent
import org.l2kserver.game.model.actor.npc.ai.MoveIntent
import org.l2kserver.game.model.actor.npc.ai.SayIntent
import org.l2kserver.game.model.actor.npc.ai.WaitIntent
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.time.GameTime
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext

@Service
class AiService(
    override val gameObjectRepository: GameObjectRepository,

    private val moveService: MoveService,
    private val combatService: CombatService,
    private val asyncTaskService: AsyncTaskService
) : AbstractService() {
    override val log = logger()

    @EventListener(ApplicationReadyEvent::class)
    fun init() = asyncTaskService.launchTask("AI_JOB") {
        while (isActive) {
            gameObjectRepository.forEachInstance<Npc> { npc ->
                if (!npc.isDead()) performAiAction(npc)
            }

            delay(GameTime.MILLIS_IN_TICK)
        }
    }

    private suspend fun performAiAction(npc: Npc) = try {
        npc.ai?.let { ai ->
            ai.onIdleAction?.let { action -> launchOnIdleAction(npc, action) }
        }
    } catch (e: Throwable) {
        log.error("An error occurred when handling {}'s ai", npc, e)
    }

    private suspend fun launchOnIdleAction(npc: Npc, action: AiIntents.(it: Npc) -> Unit) {
        if (!npc.hasAction()) npc.launchAction {
            val intents = AiIntents().apply { action(this, npc) }
            performIntendedActions(intents, npc)
        }
    }

    private suspend fun performIntendedActions(intents: AiIntents, npc: Npc) = intents.forEach { intent ->
        if (!coroutineContext.isActive) return@forEach
        when (intent) {
            is WaitIntent -> delay(intent.waitTimeMillis)
            is SayIntent -> broadcastPacket(
                ChatMessageResponse(
                    speakerId = npc.id,
                    chatTab = org.l2kserver.game.handler.dto.ChatTab.GENERAL,
                    speakerName = npc.name,
                    message = intent.message
                )
            )
            is MoveIntent -> moveService.move(npc, intent.position)
            is AttackIntent -> combatService.launchAttack(npc, intent.target.asMutable())

        }
    }

}
