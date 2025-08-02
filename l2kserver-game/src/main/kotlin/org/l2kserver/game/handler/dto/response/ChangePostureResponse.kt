package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.Posture

private const val CHANGE_POSTURE_TYPE_RESPONSE_PACKET_ID: UByte = 47u

/**
 * Notifies players about actor's posture changing
 *
 * @property actorId Identifier of actor, who has changed the posture
 * @property position Actor's position
 * @property posture Actor's new posture
 */

data class ChangePostureResponse(
    val actorId: Int,
    val position: Position,
    val posture: Posture
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CHANGE_POSTURE_TYPE_RESPONSE_PACKET_ID)

        putInt(actorId)
        putInt(posture.ordinal)

        putInt(position.x)
        putInt(position.y)
        putInt(position.z)
    }

}
