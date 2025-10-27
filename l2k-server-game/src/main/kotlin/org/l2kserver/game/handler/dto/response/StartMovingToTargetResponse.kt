package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ActorInstance

private const val START_MOVING_TO_TARGET_RESPONSE_PACKET_ID: UByte = 96u

data class StartMovingToTargetResponse(
    val actor: ActorInstance,
    val targetId: Int,
    val distance: Int
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_MOVING_TO_TARGET_RESPONSE_PACKET_ID)

        putInt(actor.id)
        putInt(targetId)
        putInt(distance)

        putInt(actor.position.x)
        putInt(actor.position.y)
        putInt(actor.position.z)
    }

}
