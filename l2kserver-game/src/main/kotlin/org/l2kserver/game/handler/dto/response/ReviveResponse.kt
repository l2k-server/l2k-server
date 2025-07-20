package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val REVIVE_RESPONSE_PACKET_ID: UByte = 7u

/**
 * Response packet to notify players about actor's revive
 */
data class ReviveResponse(val actorId: Int): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(REVIVE_RESPONSE_PACKET_ID)
        putInt(actorId)
    }

}
