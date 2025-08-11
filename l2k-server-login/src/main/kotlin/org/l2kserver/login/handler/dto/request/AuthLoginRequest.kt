package org.l2kserver.login.handler.dto.request

const val AUTH_LOGIN_REQUEST_PACKET_ID: Byte = 0x00

data class AuthLoginRequest(
    val login: String,
    val password: String
): RequestPacket
