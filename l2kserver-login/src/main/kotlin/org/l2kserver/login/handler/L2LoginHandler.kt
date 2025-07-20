package org.l2kserver.login.handler

import org.l2kserver.login.extensions.logger
import org.l2kserver.login.handler.dto.request.AuthGameGuardRequest
import org.l2kserver.login.handler.dto.request.AuthLoginRequest
import org.l2kserver.login.handler.dto.request.RequestPacket
import org.l2kserver.login.handler.dto.request.ServerListRequest
import org.l2kserver.login.handler.dto.request.SelectGameserverRequest
import org.l2kserver.login.service.LoginService
import org.springframework.stereotype.Component

@Component
class L2LoginHandler(
    private val loginService: LoginService
) {

    private val log = logger()

    suspend fun handle(sessionId: Int, request: RequestPacket) = when (request) {
        is AuthGameGuardRequest -> {
            log.debug("Got authGG packet for session = '${request.sessionId}'")
            loginService.authorizeGameGuard(sessionId, request)
        }

        is AuthLoginRequest -> {
            log.debug("Got authLogin packet of user '{}'", request.login)
            loginService.authorizeUser(sessionId, request)
        }

        is ServerListRequest -> {
            log.debug("Got ServerList request packet '{}'", request)
            loginService.getGameServers(sessionId, request)
        }

        is SelectGameserverRequest -> {
            log.debug("Got SelectGameserver request packet '{}'", request)
            loginService.selectGameserver(sessionId, request)
        }
    }

}
