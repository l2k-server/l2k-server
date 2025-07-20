package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val SELECT_CHARACTER_REQUEST_PACKET_ID: UByte = 13u

data class SelectCharacterRequest(
    val characterSlot: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(
        characterSlot = data.getInt()
    )

}
