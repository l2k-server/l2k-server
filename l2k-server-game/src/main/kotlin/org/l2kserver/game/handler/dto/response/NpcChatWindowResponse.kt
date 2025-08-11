package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte

private const val NPC_CHAT_WINDOW_RESPONSE_PACKET_ID: UByte = 15u
private const val MAX_MESSAGE_LENGTH = 8192

data class NpcChatWindowResponse(
    val npcId: Int,
    val message: String
): ResponsePacket {

    init {
        require(message.length < MAX_MESSAGE_LENGTH) { "Message must be shorter than $MAX_MESSAGE_LENGTH" }
    }

    override val data = littleEndianByteArray {
        putUByte(NPC_CHAT_WINDOW_RESPONSE_PACKET_ID)
        putInt(npcId)
        putUTF16String(message)
        putInt(0)
    }

}
