package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte

private const val MANOR_LIST_RESPONSE_PACKET_ID: UByte = 254u

data object ManorListResponse: ResponsePacket {

    private val manorList = listOf(
        "gludio",
        "dion",
        "giran",
        "oren",
        "aden",
        "innadril",
        "goddard",
        "rune",
        "schuttgart"
    )

    override val data = littleEndianByteArray {
        putUByte(MANOR_LIST_RESPONSE_PACKET_ID)
        putShort(27)

        for (i in manorList.indices) {
            putInt(i + 1)
            putUTF16String(manorList[i])
        }
    }

}
