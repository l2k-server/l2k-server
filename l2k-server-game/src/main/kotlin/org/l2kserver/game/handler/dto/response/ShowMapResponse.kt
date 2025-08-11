package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val SHOW_MAP_RESPONSE_PACKET_ID: UByte = 157u
private const val MAP_ID = 1665

data object ShowMapResponse: ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SHOW_MAP_RESPONSE_PACKET_ID)
        putInt(MAP_ID)
        putInt(0) //TODO SevenSigns period
    }

}
