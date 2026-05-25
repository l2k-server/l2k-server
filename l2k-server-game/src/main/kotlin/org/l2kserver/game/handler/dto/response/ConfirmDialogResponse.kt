package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.putUTF16String

private const val CONFIRM_DIALOG_RESPONSE_PACKET_ID: UByte = 237u

open class ConfirmDialogResponse private constructor(
    private val requestId: Int,
    private val requestorName: String
): ResponsePacket {

    final override val data = littleEndianByteArray {
        putUByte(CONFIRM_DIALOG_RESPONSE_PACKET_ID)
        putInt(requestId)
        putInt(2) // ?
        putInt(0) // ?
        putUTF16String(requestorName)
        putInt(1) //time?
        putInt(0) //requestorId?
    }

    data class Resurrection(val resurrectedBy: String): ConfirmDialogResponse(
        requestId = REQUEST_ID,
        requestorName = resurrectedBy
    ) {
        companion object {
            const val REQUEST_ID = 1510
        }
    }

}
