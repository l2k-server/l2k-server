package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray

private const val ETC_STATUS_RESPONSE_PACKET_ID: UByte = 243u
data class EtcStatusResponse(
    val chargeLevel: Int,
    val weightPenaltyLevel: Int,
    val isChatBanned: Boolean,
    val isInDangerArea: Boolean,
    val gradePenaltyLevel: Int,
    val hasCharmOfCourage: Boolean,
    val deathPenaltyLevel: Int
): ResponsePacket {

    override val data = littleEndianByteArray {
        put(ETC_STATUS_RESPONSE_PACKET_ID.toByte())
        putInt(chargeLevel)
        putInt(weightPenaltyLevel)
        putInt(if (isChatBanned) 1 else 0)
        putInt(if (isInDangerArea) 1 else 0)
        putInt(gradePenaltyLevel)
        putInt(if (hasCharmOfCourage) 1 else 0)
        putInt(deathPenaltyLevel)
    }

}
