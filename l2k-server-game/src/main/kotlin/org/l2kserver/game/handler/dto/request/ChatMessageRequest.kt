package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUTF16String
import org.l2kserver.game.handler.dto.ChatTab

const val CHAT_MESSAGE_REQUEST_PACKET_ID: UByte = 56u

/**
 * Request to write some message to chat
 *
 * @property message Message
 * @property chatTab Chat tab
 * @property targetName Name of target to send message (for private chat)
 */
data class ChatMessageRequest(
    val message: String,
    val chatTab: ChatTab,
    val targetName: String? = null
): RequestPacket

fun ChatMessageRequest(data: ByteBuffer): ChatMessageRequest {
    val message = data.getUTF16String()
    val chatTab = ChatTab.byId(data.getInt())
    val targetName = if (chatTab == ChatTab.WHISPER) data.getUTF16String() else null

    return ChatMessageRequest(message, chatTab, targetName)
}
