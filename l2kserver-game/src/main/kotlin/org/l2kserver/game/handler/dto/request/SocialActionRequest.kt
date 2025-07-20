package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.SocialAction

const val SOCIAL_ACTION_REQUEST_PACKET_ID: UByte = 27u

/**
 * Request to perform social action - greetings, dancing, etc.
 */
data class SocialActionRequest(
    val socialAction: SocialAction
): RequestPacket {
    constructor(data: ByteBuffer): this(SocialAction.byId(data.getInt()))
}

