package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.ScatteredItem

private const val PICK_UP_ITEM_RESPONSE_PACKET_ID: UByte = 13u

data class PickUpItemResponse(
    val characterId: Int,
    val item: ScatteredItem
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(PICK_UP_ITEM_RESPONSE_PACKET_ID)
        putInt(characterId)
        putInt(item.id)
        putInt(item.position.x)
        putInt(item.position.y)
        putInt(item.position.z)
    }

}
