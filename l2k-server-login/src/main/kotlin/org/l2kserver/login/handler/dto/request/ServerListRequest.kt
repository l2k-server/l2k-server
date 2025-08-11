package org.l2kserver.login.handler.dto.request

const val SERVER_LIST_REQUEST_PACKET_ID: Byte = 0x05

data class ServerListRequest(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int
): RequestPacket
