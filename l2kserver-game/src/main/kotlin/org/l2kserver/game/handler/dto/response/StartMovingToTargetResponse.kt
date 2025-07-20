package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.position.Position

private const val START_MOVING_TO_TARGET_RESPONSE_PACKET_ID: UByte = 96u

data class StartMovingToTargetResponse(
    val actorId: Int,
    val targetId: Int,
    val distance: Int,
    val actorPosition: Position
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_MOVING_TO_TARGET_RESPONSE_PACKET_ID)

        putInt(actorId)
        putInt(targetId)
        putInt(distance)

        putInt(actorPosition.x)
        putInt(actorPosition.y)
        putInt(actorPosition.z)
    }

}
