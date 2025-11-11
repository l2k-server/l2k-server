package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect

private const val ABNORMALS_LIST_RESPONSE_PACKET_ID: UByte = 127u

data class TemporalEffectsResponse(
    val abnormals: Collection<TemporalAbnormalEffect>
): ResponsePacket {

    override val data: ByteArray = littleEndianByteArray {
        putUByte(ABNORMALS_LIST_RESPONSE_PACKET_ID)
        putShort(abnormals.size.toShort())

        abnormals.forEach {
            val durationSeconds = (it.expiresAt.toEpochMilli() - System.currentTimeMillis()) / 1000
            putInt(it.skillId)
            putShort(it.effectLevel.toShort())
            putInt(durationSeconds.toInt())
        }
    }

}
