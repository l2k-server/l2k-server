package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.position.Heading
import org.l2kserver.game.model.position.Position

private const val VALIDATE_POSITION_RESPONSE_PACKET_ID: UByte = 97u

data class ValidatePositionResponse(
    val characterId: Int,
    val position: Position,
    val heading: Heading
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(VALIDATE_POSITION_RESPONSE_PACKET_ID)
        putInt(characterId)
        putInt(position.x)
        putInt(position.y)
        putInt(position.z)
        putInt(heading.toInt())
    }

}
