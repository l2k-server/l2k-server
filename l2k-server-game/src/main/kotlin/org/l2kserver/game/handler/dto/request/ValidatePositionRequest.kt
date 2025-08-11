package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position

const val VALIDATE_POSITION_REQUEST_PACKET_ID: UByte = 72u

/**
 * Request to validate character's position
 *
 * @property position Position at client side
 * @property heading Heading at client side
 */
data class ValidatePositionRequest(
    val position: Position,
    val heading: Heading
): RequestPacket {

    constructor(data: ByteBuffer): this(
        position = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        ),
        heading = Heading(data.getInt().toUShort())
    )

}
