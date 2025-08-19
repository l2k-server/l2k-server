package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.extensions.toInt
import org.l2kserver.game.model.actor.ScatteredItem

private const val SCATTERED_ITEM_RESPONSE_PACKET_ID: UByte = 11u

/**
 * Response, notifying characters that there is an item laying on the ground
 */
data class ScatteredItemResponse(
    val scatteredItem: ScatteredItem
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SCATTERED_ITEM_RESPONSE_PACKET_ID)

        putInt(scatteredItem.id)
        putInt(scatteredItem.templateId)

        putInt(scatteredItem.position.x)
        putInt(scatteredItem.position.y)
        putInt(scatteredItem.position.z)

        putInt(scatteredItem.isStackable.toInt())
        putInt(scatteredItem.amount)
    }

}
