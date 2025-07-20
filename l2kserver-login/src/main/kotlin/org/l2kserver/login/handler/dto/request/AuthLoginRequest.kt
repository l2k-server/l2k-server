package org.l2kserver.login.handler.dto.request

data class AuthLoginRequest(
    val login: String,
    val password: String
): RequestPacket
