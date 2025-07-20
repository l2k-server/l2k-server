package org.l2kserver.login.repository.domain

data class LoggedInUserData(
    val authorizationKey: AuthorizationKey,
    val state: LoggedInUserState = LoggedInUserState.LOGGED_IN
)

enum class LoggedInUserState {
    LOGGED_IN,
    CONNECTED_TO_GAME_SERVER
}
