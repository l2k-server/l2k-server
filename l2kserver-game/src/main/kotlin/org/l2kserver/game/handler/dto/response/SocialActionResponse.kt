package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.SocialAction

private const val SOCIAL_ACTION_RESPONSE_PACKET_ID: UByte = 45u

data class SocialActionResponse(
    val actorId: Int,
    val socialAction: SocialAction,
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SOCIAL_ACTION_RESPONSE_PACKET_ID)
        putInt(actorId)
        putInt(socialAction.id)
    }

}
