package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.handler.dto.response.ConfirmDialogResponse
import java.nio.ByteBuffer

const val CONFIRM_DIALOG_ANSWER_RESPONSE_PACKET_ID: UByte = 197u

sealed class ConfirmDialogAnswerRequest: RequestPacket {

    data class Resurrection(
        val confirmed: Boolean
    ): ConfirmDialogAnswerRequest()

}

fun ConfirmDialogAnswerRequest(data: ByteBuffer): ConfirmDialogAnswerRequest = when (val requestId = data.getInt()) {
    ConfirmDialogResponse.Resurrection.REQUEST_ID -> {
        val answer = data.getInt()
        ConfirmDialogAnswerRequest.Resurrection(answer > 0)
    }
    else -> error("No confirm dialogue exists by id=$requestId")
}
