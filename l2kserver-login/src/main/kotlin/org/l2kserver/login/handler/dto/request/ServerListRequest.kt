package org.l2kserver.login.handler.dto.request

data class ServerListRequest(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int
): RequestPacket
