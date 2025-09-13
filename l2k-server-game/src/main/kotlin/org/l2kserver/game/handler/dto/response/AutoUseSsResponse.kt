package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.extensions.toInt

private const val AUTO_USE_SS_EXTENDED_RESPONSE_PACKET_ID: Short = 18

data class AutoUseSsResponse(
    val ssTemplateId: Int,
    val enabled: Boolean
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(EXTENDED_RESPONSE_PACKET_ID)
        putShort(AUTO_USE_SS_EXTENDED_RESPONSE_PACKET_ID)
        putInt(ssTemplateId)
        putInt(enabled.toInt())
    }

}
