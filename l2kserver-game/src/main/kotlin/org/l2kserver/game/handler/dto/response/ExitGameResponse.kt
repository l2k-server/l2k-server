package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val EXIT_GAME_RESPONSE_PACKET_ID: UByte = 126u

data object ExitGameResponse: ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(EXIT_GAME_RESPONSE_PACKET_ID)
    }

}
