package org.l2kserver.game.model.session

data class AuthorizationKey(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int,
    val gameSessionKey1: Int,
    val gameSessionKey2: Int
)
