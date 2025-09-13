package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val AUTO_USE_SS_EXTENDED_REQUEST_PACKET_ID: UShort = 5u

/**
 * Request to enable soulshot/spiritshot automatic usage
 *
 * @property ssTemplateId Soulshot item template identifier
 * @property enable If true - enable auto usage, false - disable
 */
data class AutoUseSsRequest(val ssTemplateId: Int, val enable: Boolean): ExtendedRequestPacket {
    constructor(data: ByteBuffer): this(ssTemplateId = data.getInt(), enable = data.getInt() > 0)
}
