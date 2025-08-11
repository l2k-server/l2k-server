package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val RESTORE_CHARACTER_REQUEST_PACKET_ID: UByte = 98u

data class RestoreCharacterRequest(
    val characterSlot: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(
        characterSlot = data.getInt()
    )

}
