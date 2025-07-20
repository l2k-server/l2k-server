package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val RESTART_RESPONSE_PACKET_ID: UByte = 95u

data object RestartResponse: ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(RESTART_RESPONSE_PACKET_ID)
        putInt(1)
    }

}
