package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val CANCEL_CASTING_RESPONSE_PACKET_ID: UByte = 73u

/** Notifies client that actor has stopped casting */
data class CancelCastingResponse(val actorId: Int): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CANCEL_CASTING_RESPONSE_PACKET_ID)
        putInt(actorId)
    }

}
