package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val INITIAL_RESPONSE_PACKET_ID: UByte = 0u
data class InitialResponse(
    val key: ByteArray
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(INITIAL_RESPONSE_PACKET_ID)
        put(1)
        put(key)
        putInt(1)
        putInt(1)
    }

}
