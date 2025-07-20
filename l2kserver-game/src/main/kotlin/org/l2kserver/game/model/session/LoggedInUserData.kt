package org.l2kserver.game.model.session

data class LoggedInUserData(
    val state: LoggedInUserState,
    val authorizationKey: AuthorizationKey
) {
    fun toConnectedToGameServer(): LoggedInUserData {
        check(this.state == LoggedInUserState.LOGGED_IN) { "Wrong state of user ${this.state}" }
        return this.copy(state = LoggedInUserState.CONNECTED_TO_GAME_SERVER)
    }
}

enum class LoggedInUserState {
    LOGGED_IN,
    CONNECTED_TO_GAME_SERVER
}
