package org.l2kserver.login.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.l2kserver.login.configuration.GameserverSettings
import org.l2kserver.login.exception.L2LoginException
import org.l2kserver.login.extensions.domain.create
import org.l2kserver.login.extensions.domain.findByLogin
import org.l2kserver.login.extensions.domain.updateLastActive
import org.l2kserver.login.extensions.logger
import org.l2kserver.login.handler.dto.request.AuthGameGuardRequest
import org.l2kserver.login.handler.dto.request.AuthLoginRequest
import org.l2kserver.login.handler.dto.request.SelectGameserverRequest
import org.l2kserver.login.handler.dto.request.ServerListRequest
import org.l2kserver.login.handler.dto.response.AuthFailedResponse
import org.l2kserver.login.handler.dto.response.AuthGameGuardResponse
import org.l2kserver.login.handler.dto.response.AuthSuccessResponse
import org.l2kserver.login.handler.dto.response.GameServerInfo
import org.l2kserver.login.handler.dto.response.ResponsePacket
import org.l2kserver.login.handler.dto.response.SelectGameserverFailedResponse
import org.l2kserver.login.handler.dto.response.SelectGameserverSuccessResponse
import org.l2kserver.login.handler.dto.response.ServerListResponse
import org.l2kserver.login.handler.dto.response.enums.FailReason
import org.l2kserver.login.repository.LoggedInUsersRepository
import org.l2kserver.login.repository.SessionRepository
import org.l2kserver.login.domain.Account
import org.l2kserver.login.repository.domain.LoggedInUserData
import org.l2kserver.login.repository.domain.LoggedInUserState
import org.l2kserver.login.repository.domain.AuthorizationKey
import org.l2kserver.login.utils.PasswordUtils
import org.l2kserver.login.utils.ServerUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class LoginService(
    private val sessionRepository: SessionRepository,
    private val loggedInUsersRepository: LoggedInUsersRepository,
    private val gameserverSettings: List<GameserverSettings>,

    @Value("\${server.automaticRegistrationEnabled}")
    private val autoRegistrationEnabled: Boolean,
    @Value("\${server.connectToGameServerTimeout}")
    private val connectionToGameServerTimeout: Long
) {

    private val log = logger()

    /**
     * Pretends to authorize GameGuard
     */
    fun authorizeGameGuard(sessionId: Int, request: AuthGameGuardRequest): ResponsePacket {
        val sessionData = sessionRepository.findById(sessionId)
        check(request.sessionId == sessionId) { "Wrong session id ${request.sessionId}" }

        sessionRepository.save(sessionData.toAuthorizedGameGuard())
        return AuthGameGuardResponse(request.sessionId)
    }

    /**
     * Authorizes user
     *
     * @param request Login request, containing username and password
     *
     * @return AuthSuccessResponse is user is successfully authorized
     */
    suspend fun authorizeUser(sessionId: Int, request: AuthLoginRequest): ResponsePacket {
        log.debug("Authorizing user '{}'", request.login)
        var account = Account.findByLogin(request.login)
        val sessionData = sessionRepository.findById(sessionId)

        if (account == null && autoRegistrationEnabled) {
            account = Account.create(request.login, request.password)
        }

        return if (PasswordUtils.encode(request.password) == account?.password) {
            if (sessionRepository.existsByLogin(account.login) ||
                loggedInUsersRepository.existsByLogin(account.login)
            ) {
                //TODO Kick account?
                log.debug("Account '{}' is in use", request.login)
                return AuthFailedResponse(FailReason.ACCOUNT_IN_USE)
            }
            val sessionKey = AuthorizationKey()
            sessionRepository.save(sessionData.toAuthorized(account.login, sessionKey))
            account.updateLastActive()
            log.info("User '{}' successfully authorized", request.login)

            AuthSuccessResponse(sessionKey.loginSessionKey1, sessionKey.loginSessionKey2)
        } else {
            log.debug("'{}' Wrong login or password", request.login)
            AuthFailedResponse(FailReason.USER_OR_PASS_WRONG)
        }
    }

    /**
     * Finds registered gameservers
     *
     * @param sessionId Id of user session
     * @param request ServerListRequest with session keys
     * @return ServerListResponse with game servers info
     * @throws L2LoginException if user was not correctly authorized
     */
    suspend fun getGameServers(sessionId: Int, request: ServerListRequest): ServerListResponse {
        val session = sessionRepository.findById(sessionId)
        checkSessionKey(request.loginSessionKey1, request.loginSessionKey2, session.sessionKey)

        val account = Account.findByLogin(session.login!!) ?: throw L2LoginException(
            "Account '$session.login' was not found in db ¯\\_(ツ)_/¯"
        )

        return ServerListResponse(
            lastServerId = (account.lastServer ?: 1).toByte(),
            servers = gameserverSettings.map { gameserverInfo ->
                GameServerInfo(
                    id = gameserverInfo.id,
                    serverIp = gameserverInfo.ip.split('.').map { it.toByte() }.toByteArray(),
                    port = gameserverInfo.port,
                    ageLimit = gameserverInfo.ageLimit,
                    isPvp = gameserverInfo.isPvp,
                    currentPlayers = 0, //TODO find in redis
                    maxPlayers = gameserverInfo.maxPlayers,
                    isOnline = account.accessLevel >= gameserverInfo.accessLevel
                            && ServerUtils.checkOnline(gameserverInfo.ip, gameserverInfo.port),
                    showBrackets = false //TODO HZ
                )
            }
        )
    }

    /**
     * Creates GameSession for this account and returns packet with gameSessionKey
     *
     * @param request SelectedGameserverRequest
     * @return SelectGameserversSuccessResponse with game session keys
     * @throws L2LoginException if user was not correctly authorized
     */
    suspend fun selectGameserver(sessionId: Int, request: SelectGameserverRequest): ResponsePacket {
        val currentOnline = 0 //TODO find in redis
        val gameserverInfo = gameserverSettings.find { it.id == request.selectedGameserverId }
            ?: throw L2LoginException("Selected gameserver is not registered")

        val maxPlayers = gameserverInfo.maxPlayers

        if (currentOnline >= maxPlayers) {
            log.warn("Server '${request.selectedGameserverId}' is overloaded")
            return SelectGameserverFailedResponse(FailReason.SERVER_OVERLOADED)
        }

        val session = sessionRepository.findById(sessionId)
        checkSessionKey(request.loginSessionKey1, request.loginSessionKey2, session.sessionKey)

        val account = Account.findByLogin(session.login!!)
            ?: throw L2LoginException("Account '${session.login}' was not found in db ¯\\_(ツ)_/¯")

        if (account.accessLevel < gameserverInfo.accessLevel) {
            log.debug("User '{}' is not allowed to join gameserver '{}'", account.login, gameserverInfo.id)
            return SelectGameserverFailedResponse(FailReason.ACCESS_FAILED)
        }

        sessionRepository.delete(session)
        loggedInUsersRepository.save(account.login, gameserverInfo.name, LoggedInUserData(session.sessionKey!!))

        launchCleaningJob(account.login, gameserverInfo.name)

        return SelectGameserverSuccessResponse(
            session.sessionKey.gameSessionKey1, session.sessionKey.gameSessionKey2
        )
    }

    /**
     * Launches a job, which deletes logged-in user, if it was not connected to game server in time
     */
    private suspend fun launchCleaningJob(
        login: String, gameserverName: String
    ) = CoroutineScope(NonCancellable).launch {
        delay(connectionToGameServerTimeout)
        loggedInUsersRepository.findByLogin(login, gameserverName)?.let {
            if (it.state != LoggedInUserState.CONNECTED_TO_GAME_SERVER) {
                log.warn(
                    "Player '{}' has not connected to Game Server in $connectionToGameServerTimeout ms", login
                )
                loggedInUsersRepository.deleteByLogin(login, gameserverName)
            }
        }
    }

    private fun checkSessionKey(loginSessionKey1: Int, loginSessionKey2: Int, sessionKey: AuthorizationKey?) {
        if (loginSessionKey1 != sessionKey?.loginSessionKey1 || loginSessionKey2 != sessionKey.loginSessionKey2)
            throw L2LoginException("Wrong session key '${loginSessionKey1}:${loginSessionKey2}'")
    }

}
