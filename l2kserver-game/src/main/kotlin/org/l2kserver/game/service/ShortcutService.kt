package org.l2kserver.game.service

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.DeleteShortcutRequest
import org.l2kserver.game.handler.dto.request.CreateShortcutRequest
import org.l2kserver.game.handler.dto.response.CreateShortcutResponse
import org.l2kserver.game.repository.GameObjectDAO
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.extensions.model.shortcut.create
import org.l2kserver.game.extensions.model.shortcut.deleteBy
import org.l2kserver.game.extensions.model.shortcut.findBy
import org.l2kserver.game.extensions.model.skill.findBy
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.springframework.stereotype.Service

@Service
class ShortcutService(
    override val gameObjectDAO: GameObjectDAO
) : AbstractService() {

    override val log = logger()

    suspend fun registerShortcut(request: CreateShortcutRequest) = newSuspendedTransaction {
        val character = gameObjectDAO.findCharacterById(sessionContext().getCharacterId())

        Shortcut.findBy(request.index, character.id, character.activeSubclass)?.delete()

        val actionLevel = if (request.type == ShortcutType.SKILL)
            Skill.findBy(request.shortcutActionId, character.id, character.activeSubclass).skillLevel
        else 1

        val newShortcut = Shortcut.create(
            characterId = character.id,
            subclassIndex = character.activeSubclass,
            shortcutIndex = request.index,
            shortcutType = request.type,
            shortcutActionId = request.shortcutActionId,
            shortcutActionLevel = actionLevel
        )

        send(CreateShortcutResponse(newShortcut))
        log.info("Registered new shortcut '{}'", newShortcut)
    }

    suspend fun deleteShortcut(request: DeleteShortcutRequest) = newSuspendedTransaction {
        val character = gameObjectDAO.findCharacterById(sessionContext().getCharacterId())
        Shortcut.deleteBy(character.id, character.activeSubclass, request.index)

        log.info("Successfully deleted shortcut with index {} of character {}", request.index, character)
    }

}
