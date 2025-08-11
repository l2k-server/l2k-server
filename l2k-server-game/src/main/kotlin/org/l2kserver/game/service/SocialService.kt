package org.l2kserver.game.service

import org.l2kserver.game.extensions.forEachInstanceMatching
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.ChatMessageRequest
import org.l2kserver.game.handler.dto.response.ChatMessageResponse
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.model.map.Town
import org.l2kserver.game.handler.dto.ChatTab
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sendTo
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Works on characters' social activities - talking, emotions, etc.
 */
@Service
class SocialService(
    override val gameObjectRepository: GameObjectRepository,

    @Value("\${chat.general_chat_range}") private val generalChatRange: Int,
): AbstractService() {

    override val log = logger()

    suspend fun handleChatMessageRequest(request: ChatMessageRequest) {
        val speaker = gameObjectRepository.findActorById(sessionContext().getCharacterId())
        log.debug("Player '{}' tries to say '{}'", speaker, request.message)

        if (request.message.isBlank()) return
        //TODO Check chat banned and censorship

        val response = ChatMessageResponse(
            speakerId = speaker.id,
            chatTab = request.chatTab,
            speakerName = speaker.name,
            message = request.message
        )

        when (request.chatTab) {
            ChatTab.GENERAL -> broadcastPacket(response, speaker.position, generalChatRange)
            ChatTab.WHISPER -> {
                if (request.targetName != null) {
                    send(response)
                    sendTo(gameObjectRepository.findCharacterByName(request.targetName).id, response)
                }
            }
            ChatTab.SHOUT, ChatTab.TRADE -> {
                val closestTown = Town.Registry.findClosestByPosition(speaker.position)
                gameObjectRepository.forEachInstanceMatching<PlayerCharacter>(
                    predicate = { Town.Registry.findClosestByPosition(it.position) == closestTown },
                    action = { sendTo(it.id, response) }
                )
            }

            else -> send(SystemMessageResponse("${request.chatTab} chat is not supported yet"))
        }

    }

}
