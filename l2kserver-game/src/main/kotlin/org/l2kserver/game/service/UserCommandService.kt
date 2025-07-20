package org.l2kserver.game.service

import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.UserCommand
import org.l2kserver.game.handler.dto.request.UserCommandRequest
import org.l2kserver.game.handler.dto.response.SystemMessageResponse
import org.l2kserver.game.domain.map.Town
import org.l2kserver.game.network.session.send
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.repository.GameObjectDAO
import org.springframework.stereotype.Service

@Service
class UserCommandService(
    override val gameObjectDAO: GameObjectDAO
) : AbstractService() {

    override val log = logger()

    suspend fun handleUserCommand(request: UserCommandRequest) {
        val context = sessionContext()
        log.debug("Got command '{}' of user with 'session' {}", request.command, context.sessionId)

        when (request.command) {
            UserCommand.LOC -> handleLocCommand()
            else -> send(SystemMessageResponse("Got command '${request.command}'"))
        }
    }

    suspend fun handleLocCommand() {
        val context = sessionContext()
        val position = gameObjectDAO.findCharacterById(context.getCharacterId()).position

        val closestTown = Town.findClosestByPosition(position)

        send(
            SystemMessageResponse(
                "Current location: ${position.x}, ${position.y}, ${position.z} (Near ${closestTown.name})"
            )
        )
    }

}
