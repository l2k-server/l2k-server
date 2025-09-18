package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.position.Heading

private const val STOP_ROTATION_RESPONSE_PACKET_ID: UByte = 99u

data class StopRotationResponse(
    val actorId: Int,
    val heading: Heading
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(STOP_ROTATION_RESPONSE_PACKET_ID)
        putInt(actorId)
        putInt(heading.value.toInt())
    }
}
