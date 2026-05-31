package org.l2kserver.game.service

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.DeleteShortcutRequest
import org.l2kserver.game.handler.dto.request.CreateShortcutRequest
import org.l2kserver.game.handler.dto.response.CreateShortcutResponse
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.ShortcutRepository
import org.springframework.stereotype.Service

@Service
class ShortcutService(
    override val gameObjectRepository: GameObjectRepository,
    private val shortcutRepository: ShortcutRepository
) : AbstractService() {

    override val log = logger()

    suspend fun registerShortcut(request: CreateShortcutRequest) = suspendTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        shortcutRepository.findBy(
            request.index, character.id, character.activeSubclass
        )?.delete()

        val actionLevel = if (request.type == ShortcutType.SKILL)
            character.skillsAndMagic.findById(request.shortcutActionId).skillLevel
        else 1

        val newShortcut = shortcutRepository.create(
            characterId = character.id,
            subclassIndex = character.activeSubclass,
            shortcutIndex = request.index,
            shortcutType = request.type,
            shortcutActionId = request.shortcutActionId,
            shortcutActionLevel = actionLevel
        )

        send { CreateShortcutResponse(newShortcut) }
        log.info { "Registered new shortcut '$newShortcut'" }
    }

    suspend fun deleteShortcut(request: DeleteShortcutRequest) = suspendTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        shortcutRepository.deleteBy(character.id, character.activeSubclass, request.index)

        log.info { "Successfully deleted shortcut with index ${request.index} of character $character" }
    }

}
