package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toInt
import org.l2kserver.game.model.actor.ScatteredItem

private const val DROPPED_ITEM_RESPONSE_PACKET_ID: UByte = 12u

/**
 * Response, notifying nearby characters that item was just dropped
 */
data class DroppedItemResponse(
    val dropperId: Int,
    val scatteredItem: ScatteredItem
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(DROPPED_ITEM_RESPONSE_PACKET_ID)
        putInt(dropperId)
        putInt(scatteredItem.id)
        putInt(scatteredItem.templateId)

        putInt(scatteredItem.position.x)
        putInt(scatteredItem.position.y)
        putInt(scatteredItem.position.z)

        putInt(scatteredItem.isStackable.toInt())
        putInt(scatteredItem.amount)
        putInt(1)
    }

}
