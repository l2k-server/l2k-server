package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.position.Position

private const val CANCEL_TARGET_RESPONSE_PACKET_ID: UByte = 42u

/**
 * Cancels player's casting or target, if player is not casting.
 */
data class CancelActionResponse(
    val characterId: Int,
    val characterPosition: Position
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CANCEL_TARGET_RESPONSE_PACKET_ID)
        putInt(characterId)
        putInt(characterPosition.x)
        putInt(characterPosition.y)
        putInt(characterPosition.z)
    }

}
