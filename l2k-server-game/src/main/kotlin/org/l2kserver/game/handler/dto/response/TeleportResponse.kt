package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.position.Position

private const val TELEPORT_RESPONSE_PACKET_ID: UByte = 40u

/**
 * Notifies client about actor has teleported to some position
 *
 * @property actorId Teleported actor identifier
 * @property position Teleport target position
 */
data class TeleportResponse(val actorId: Int, val position: Position): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(TELEPORT_RESPONSE_PACKET_ID)
        putInt(actorId)

        putInt(position.x)
        putInt(position.y)
        putInt(position.z)
    }

}
