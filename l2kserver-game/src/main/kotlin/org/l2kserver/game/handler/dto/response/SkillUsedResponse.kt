package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.position.Position

private const val SKILL_USED_RESPONSE_PACKET_ID: UByte = 72u

data class SkillUsedResponse(
    val casterId: Int,
    val targetId: Int,
    val skillId: Int,
    val skillLevel: Int,
    val castTime: Int,
    val reuseDelay: Int,
    val casterPosition: Position
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SKILL_USED_RESPONSE_PACKET_ID)
        putInt(casterId)
        putInt(targetId)
        putInt(skillId)
        putInt(skillLevel)
        putInt(castTime)
        putInt(reuseDelay)
        putInt(casterPosition.x)
        putInt(casterPosition.y)
        putInt(casterPosition.z)
    }

}
