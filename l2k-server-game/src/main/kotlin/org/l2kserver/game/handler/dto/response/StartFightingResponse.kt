package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val START_FIGHTING_RESPONSE_PACKET_ID: UByte = 43u

/**
 * Notify that actor with [actorId] is now in combat stance
 */
data class StartFightingResponse(val actorId: Int): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_FIGHTING_RESPONSE_PACKET_ID)
        putInt(actorId)
    }

}
