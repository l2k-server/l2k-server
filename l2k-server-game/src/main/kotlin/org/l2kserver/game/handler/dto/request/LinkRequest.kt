package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.extensions.getUTF16String
import java.nio.ByteBuffer

const val LINK_REQUEST_PACKET_ID: UByte = 32u

data class LinkRequest(
    val link: String
): RequestPacket {
    constructor(data: ByteBuffer): this(link = data.getUTF16String())
}
