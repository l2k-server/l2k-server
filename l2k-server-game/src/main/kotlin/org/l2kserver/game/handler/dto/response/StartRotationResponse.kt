package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.handler.dto.request.RotationDirection
import org.l2kserver.game.model.actor.position.Heading

private const val START_ROTATION_RESPONSE_PACKET_ID: UByte = 98u

data class StartRotationResponse(
    val actorId: Int,
    val heading: Heading,
    val direction: RotationDirection
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_ROTATION_RESPONSE_PACKET_ID)
        putInt(actorId)
        putInt(heading.value.toInt())
        putInt(direction.value)
        putInt(0)
    }

}
