package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val SET_TARGET_RESPONSE_PACKET_ID: UByte = 166u

data class SetTargetResponse(
    val targetId: Int,
    val levelDifference: Int = 0
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SET_TARGET_RESPONSE_PACKET_ID)
        putInt(targetId)
        putShort(levelDifference.toShort())
    }

}
