package org.l2kserver.login.handler.dto.request

data class SelectGameserverRequest(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int,
    val selectedGameserverId: Byte
): RequestPacket
