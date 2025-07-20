package org.l2kserver.game.service

import org.l2kserver.game.extensions.logger
import org.l2kserver.game.handler.dto.request.AuthorizationRequest
import org.l2kserver.game.handler.dto.request.InitialRequest
import org.l2kserver.game.handler.dto.response.InitialResponse
import org.l2kserver.game.network.session.sessionContext
import org.l2kserver.game.network.session.LoggedInUsersRepository
import org.l2kserver.game.network.session.send
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
    private val characterService: CharacterService,
    private val loggedInUsersRepository: LoggedInUsersRepository,

    @Value("\${server.protocolVersion}")
    private val acceptableProtocolVersion: Int
) {

    private val log = logger()

    suspend fun checkProtocolVersion(request: InitialRequest, key: ByteArray) {
        require(request.protocolVersion == acceptableProtocolVersion) {
            "Protocol version ${request.protocolVersion} is not supported"
        }

        send(InitialResponse(key))
    }

    /**
     * Checks the player is authorized at login server and
     */
    suspend fun authorize(request: AuthorizationRequest) {
        log.debug("User {} is trying to login", request.login)
        val loggedInUser = checkNotNull(loggedInUsersRepository.findByLogin(request.login)) {
            "User ${request.login} was not correctly authorized"
        }

        check(request.authorizationKey == loggedInUser.authorizationKey) {
            "Wrong authorization key ${request.authorizationKey}"
        }

        loggedInUsersRepository.save(request.login, loggedInUser.toConnectedToGameServer())
        val sessionContext = sessionContext()
        sessionContext.setAccountName(request.login)
        sessionContext.setAuthorizationKey(loggedInUser.authorizationKey)

        log.info("User {} is successfully authorized", request.login)

        characterService.sendCharactersList()
    }

    /**
     * Logs out owner of current session
     */
    suspend fun logOut() {
        sessionContext().getAccountNameOrNull()?.let { loggedInUsersRepository.deleteByLogin(it) }
    }

}
