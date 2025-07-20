package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.position.Position

private const val START_MOVING_RESPONSE_PACKET_ID: UByte = 1u

data class StartMovingResponse(
    val characterId: Int,
    val position: Position,
    val destination: Position
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_MOVING_RESPONSE_PACKET_ID)

        putInt(characterId)

        putInt(destination.x)
        putInt(destination.y)
        putInt(destination.z)

        putInt(position.x)
        putInt(position.y)
        putInt(position.z)
    }

}
