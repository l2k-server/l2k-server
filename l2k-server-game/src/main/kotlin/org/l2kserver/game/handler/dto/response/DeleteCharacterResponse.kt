package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val DELETE_CHARACTER_FAIL_RESPONSE_PACKET_ID: UByte = 36u
data class DeleteCharacterFailResponse(
    val reason: DeleteCharacterFailReason
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(DELETE_CHARACTER_FAIL_RESPONSE_PACKET_ID)
        putInt(reason.code)
    }

}

enum class DeleteCharacterFailReason(val code: Int) {
    DELETION_FAILED(code = 1),
    YOU_MAY_NOT_DELETE_CLAN_MEMBER(code = 2),
    CLAN_LEADERS_MAY_NOT_BE_DELETED(code = 3)
}
