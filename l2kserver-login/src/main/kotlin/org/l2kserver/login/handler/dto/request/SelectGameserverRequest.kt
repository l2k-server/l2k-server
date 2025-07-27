package org.l2kserver.login.handler.dto.request

const val SELECT_GAMESERVER_REQUEST_PACKET_ID: Byte = 0x02

data class SelectGameserverRequest(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int,
    val selectedGameserverId: Byte
): RequestPacket
