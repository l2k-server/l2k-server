package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val CREATE_CHARACTER_FAIL_RESPONSE_PACKET_ID: UByte = 26u

data class CreateCharacterFailResponse(
    val reason: CreateCharacterFailReason
) : ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CREATE_CHARACTER_FAIL_RESPONSE_PACKET_ID)
        putInt(reason.code)
    }

}

enum class CreateCharacterFailReason(val code: Int) {
    CREATION_FAILED(0),
    TOO_MANY_CHARACTERS(1),
    NAME_ALREADY_EXISTS(2),
    NAME_EXCEED_16_CHARACTERS(3);
}
