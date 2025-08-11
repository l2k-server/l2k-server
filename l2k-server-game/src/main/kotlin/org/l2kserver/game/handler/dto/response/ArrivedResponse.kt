package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position

private const val ARRIVED_RESPONSE_PACKET_ID: UByte = 71u

data class ArrivedResponse(
    val actorId: Int,
    val position: Position,
    val heading: Heading
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ARRIVED_RESPONSE_PACKET_ID)
        putInt(actorId)
        putInt(position.x)
        putInt(position.y)
        putInt(position.z)
        putInt(heading.toInt())
    }

}
