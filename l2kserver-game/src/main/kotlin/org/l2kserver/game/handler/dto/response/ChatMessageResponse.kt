package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.handler.dto.ChatTab

private const val CHAT_MESSAGE_RESPONSE_PACKET_ID: UByte = 74u
data class ChatMessageResponse(
    val speakerId: Int,
    val chatTab: ChatTab,
    val speakerName: String,
    val message: String
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(CHAT_MESSAGE_RESPONSE_PACKET_ID)
        putInt(speakerId)
        putInt(chatTab.id)
        putUTF16String(speakerName)
        putUTF16String(message)
    }

}
