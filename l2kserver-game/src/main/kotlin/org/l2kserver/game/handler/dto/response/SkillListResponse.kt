package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.model.skill.SkillType
import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toInt
import org.l2kserver.game.model.skill.Skill

private const val SKILL_LIST_RESPONSE_PACKET_ID: UByte = 88u

data class SkillListResponse(
    val skills: Collection<Skill>
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SKILL_LIST_RESPONSE_PACKET_ID)
        putInt(skills.size)

        skills.forEach {
            putInt((it.skillType == SkillType.PASSIVE).toInt())
            putInt(it.skillLevel)
            putInt(it.skillId)
            put(0)
        }
    }

}
