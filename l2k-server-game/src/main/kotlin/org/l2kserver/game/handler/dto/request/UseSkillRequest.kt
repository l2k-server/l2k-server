package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val USE_SKILL_REQUEST_PACKET_ID: UByte = 47u

/**
 * Request to use skill
 *
 * @property skillId Used skill identifier
 * @property forced Is this skill forced to use (ctrl pressed)
 * @property holdPosition Do not move to target, if range is too far to use skill (shift pressed)
 */
data class UseSkillRequest(
    val skillId: Int,
    val forced: Boolean,
    val holdPosition: Boolean
): RequestPacket {

    constructor(data: ByteBuffer): this(
        skillId = data.getInt(),
        forced = data.getInt() != 0,
        holdPosition = data.get().toInt() != 0
    )

}
