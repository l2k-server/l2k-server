package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val STOP_FIGHTING_RESPONSE_PACKET_ID: UByte = 44u

/**
 * Notify that actor with [actorId] is now not in combat stance
 */
data class StopFightingResponse(val actorId: Int): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(STOP_FIGHTING_RESPONSE_PACKET_ID)
        putInt(actorId)
    }

}
