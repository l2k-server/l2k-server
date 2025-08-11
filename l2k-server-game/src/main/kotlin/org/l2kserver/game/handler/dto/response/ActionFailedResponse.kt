package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val  ACTION_TAILED_RESPONSE_PACKET_ID: UByte = 37u

data object ActionFailedResponse: ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ACTION_TAILED_RESPONSE_PACKET_ID)
    }

}
