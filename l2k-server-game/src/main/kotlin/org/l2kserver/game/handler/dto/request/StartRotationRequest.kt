package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.model.actor.position.Heading
import java.nio.ByteBuffer

const val START_ROTATION_REQUEST_PACKET_ID: UByte = 74u

/**
 * Request to rotate character (by arrows when not moving)
 *
 * @property currentHeading character's current heading direction
 * @property rotationDirection side to turn to
 */
data class StartRotationRequest(
    val currentHeading: Heading,
    val rotationDirection: RotationDirection
) : RequestPacket {

    constructor(data: ByteBuffer) : this(
        currentHeading = Heading(data.getInt()),
        rotationDirection = data.getInt().toRotationDirection()
    )

}

enum class RotationDirection(val value: Int) {
    LEFT(-1),
    RIGHT(1)
}

private fun Int.toRotationDirection() = when (this) {
    RotationDirection.LEFT.value -> RotationDirection.LEFT
    RotationDirection.RIGHT.value -> RotationDirection.RIGHT
    else -> throw IllegalArgumentException("Cannot determine rotation direction by value '$this'")
}
