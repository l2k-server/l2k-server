package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.actor.position.Position

const val MOVE_REQUEST_PACKET_ID: UByte = 1u

/**
 * Request to move character
 *
 * @property targetPosition Requested position to move to
 * @property characterPosition Current character position at client side
 * @property byMouse true if player moved by mouse, else if not
 */
data class MoveRequest(
    val targetPosition: Position,
    val characterPosition: Position,
    val byMouse: Boolean
): RequestPacket {
    
    constructor(data: ByteBuffer): this(
        targetPosition = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        ),
        characterPosition = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        ),
        byMouse = data.getInt() != 0
    )

}
