package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.item.ShotInstance
import org.l2kserver.game.model.item.SoulshotInstance
import org.l2kserver.game.model.item.SpiritshotInstance
import org.l2kserver.game.model.item.Grade

private const val SKILL_USED_RESPONSE_PACKET_ID: UByte = 72u

/**
 * Notifies client that actor has used skill
 */
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

/**
 * Notifies client that character has used soul- or spiritshot
 */
data class ShotUsedResponse(
    val usedBy: PlayerCharacterInstance,
    val shot: ShotInstance
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SKILL_USED_RESPONSE_PACKET_ID)
        putInt(usedBy.id)
        putInt(usedBy.id)
        putInt(when(shot) {
            is SoulshotInstance -> getSoulshotUsageSkillId(shot.grade)
            is SpiritshotInstance -> getSpiritshotUsageSkillId(shot.grade)
        })
        putInt(1)
        putInt(0)
        putInt(0)
        putInt(usedBy.position.x)
        putInt(usedBy.position.y)
        putInt(usedBy.position.z)
    }

    private fun getSoulshotUsageSkillId(grade: Grade) = when(grade) {
        Grade.NO_GRADE -> 2039
        Grade.D -> 2150
        Grade.C -> 2151
        Grade.B -> 2152
        Grade.A -> 2153
        Grade.S -> 2154
    }

    private fun getSpiritshotUsageSkillId(grade: Grade) = when(grade) {
        Grade.NO_GRADE -> 2061
        Grade.D -> 2155
        Grade.C -> 2156
        Grade.B -> 2157
        Grade.A -> 2158
        Grade.S -> 2159
    }

}
