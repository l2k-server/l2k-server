package org.l2kserver.login.handler.dto.request

const val AUTH_GAME_GUARD_REQUEST_PACKET_ID: Byte = 0x07

data class AuthGameGuardRequest(
    val sessionId: Int
): RequestPacket
