package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val SCALE_RESPONSE_PACKET_ID: UByte = 109u

/**
 * Makes client to display Gauge of given [gaugeColor] for given [time] period
 */
data class GaugeResponse(
    val gaugeColor: GaugeColor,
    val time: Int
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SCALE_RESPONSE_PACKET_ID)
        putInt(gaugeColor.ordinal)
        putInt(time)
        putInt(time)
    }

}

enum class GaugeColor {
    /**
     * Used for skill casting scale
     */
    BLUE,

    /**
     * Used for arrow launching scale
     */
    RED,

    /**
     * Used for breath scale
     */
    CYAN
}
