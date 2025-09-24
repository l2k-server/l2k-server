package org.l2kserver.game.service

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.DeleteShortcutRequest
import org.l2kserver.game.handler.dto.request.CreateShortcutRequest
import org.l2kserver.game.handler.dto.response.CreateShortcutResponse
import org.l2kserver.game.repository.GameObjectRepository
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.ShortcutRepository
import org.l2kserver.game.repository.SkillRepository
import org.springframework.stereotype.Service

@Service
class ShortcutService(
    override val gameObjectRepository: GameObjectRepository,
    private val shortcutRepository: ShortcutRepository,
    private val skillRepository: SkillRepository
) : AbstractService() {

    override val log = logger()

    suspend fun registerShortcut(request: CreateShortcutRequest) = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())

        shortcutRepository.findBy(
            request.index, character.id, character.activeSubclass
        )?.delete()

        val actionLevel = if (request.type == ShortcutType.SKILL)
            skillRepository.findBy(
                request.shortcutActionId, character.id, character.activeSubclass
            ).skillLevel
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
        log.info("Registered new shortcut '{}'", newShortcut)
    }

    suspend fun deleteShortcut(request: DeleteShortcutRequest) = newSuspendedTransaction {
        val character = gameObjectRepository.findCharacterById(sessionContext().getCharacterId())
        shortcutRepository.deleteBy(character.id, character.activeSubclass, request.index)

        log.info("Successfully deleted shortcut with index {} of character {}", request.index, character)
    }

}
