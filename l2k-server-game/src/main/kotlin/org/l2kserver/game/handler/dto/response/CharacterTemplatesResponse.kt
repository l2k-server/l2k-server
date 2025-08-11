package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val CHARACTER_TEMPLATES_RESPONSE_PACKET_ID: UByte = 23u
data object CharacterTemplatesResponse: ResponsePacket {
    override val data = littleEndianByteArray {
        putUByte(CHARACTER_TEMPLATES_RESPONSE_PACKET_ID)
        putInt(0)
        //here can be characterTemplates data, but it is unused
    }

}
