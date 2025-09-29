package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.extensions.getUTF16String
import java.nio.ByteBuffer

const val BYPASS_TO_SERVER_REQUEST_PACKET_ID: UByte = 33u

data class BypassToServerRequest(
    val bypassCommandString: String
): RequestPacket {
    constructor(data: ByteBuffer): this(bypassCommandString = data.getUTF16String())
}
