package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.position.Position

private const val START_MOVING_RESPONSE_PACKET_ID: UByte = 1u

data class StartMovingResponse(
    val actor: ActorInstance,
    val destination: Position
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(START_MOVING_RESPONSE_PACKET_ID)

        putInt(actor.id)

        putInt(destination.x)
        putInt(destination.y)
        putInt(destination.z)

        putInt(actor.position.x)
        putInt(actor.position.y)
        putInt(actor.position.z)
    }

}
