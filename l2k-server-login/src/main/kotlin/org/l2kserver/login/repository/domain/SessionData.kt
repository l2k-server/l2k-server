package org.l2kserver.login.repository.domain

/**
 * User session data
 *
 * @param sessionId Id of this session
 * @param login User name
 * @param sessionKey Session key
 * @param selectedGameserverId Id of game server, to which the user is going to connect
 * @param sessionState State of user session
 */
data class SessionData(
    val sessionId: Int,
    val login: String? = null,
    val sessionKey: AuthorizationKey? = null,
    val selectedGameserverId: Byte? = null,
    val sessionState: SessionState = SessionState.CONNECTED
) {

    fun toAuthorizedGameGuard(): SessionData {
        check(this.sessionState == SessionState.CONNECTED) {
            "Wrong session state ${this.sessionState} to authorize game guard!"
        }

        return this.copy(sessionState = SessionState.AUTHORIZED_GAME_GUARD)
    }

    fun toAuthorized(login: String, sessionKey: AuthorizationKey?): SessionData {
        check(this.sessionState == SessionState.AUTHORIZED_GAME_GUARD) {
            "Wrong session state ${this.sessionState} to authorize user!"
        }

        return this.copy(
            login = login,
            sessionKey = sessionKey,
            sessionState = SessionState.AUTHORIZED
        )
    }
}

enum class SessionState{
    CONNECTED,
    AUTHORIZED_GAME_GUARD,
    AUTHORIZED
}
