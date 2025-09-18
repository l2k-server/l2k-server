package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.model.actor.position.Heading
import java.nio.ByteBuffer

const val STOP_ROTATION_REQUEST_PACKET_ID: UByte = 75u

data class StopRotationRequest(
    val heading: Heading,
    val unknown: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(
        heading = Heading(data.getInt()),
        unknown = data.getInt()
    )
}
