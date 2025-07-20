package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toByte
import org.l2kserver.game.model.actor.enumeration.MoveType

private const val CHANGE_MOVE_TYPE_RESPONSE_PACKET_ID: UByte = 46u

data class ChangeMoveTypeResponse(
    val actorId: Int,
    val moveType: MoveType
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CHANGE_MOVE_TYPE_RESPONSE_PACKET_ID)
        putInt(actorId)
        put((moveType == MoveType.RUN).toByte())
    }

}
