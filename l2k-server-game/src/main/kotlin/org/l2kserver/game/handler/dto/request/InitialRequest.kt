package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val INITIAL_REQUEST_PACKET_ID: UByte = 0u

data class InitialRequest(
    val protocolVersion: Int
): RequestPacket {

    //Initial request has more data but it is unused
    constructor(data: ByteBuffer): this(
        protocolVersion = data.getInt()
    )

}
