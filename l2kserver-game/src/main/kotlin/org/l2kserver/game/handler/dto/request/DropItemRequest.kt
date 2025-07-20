package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.model.position.Position

const val DROP_ITEM_REQUEST_PACKET_ID: UByte = 18u

/**
 * Request to drop item on the ground
 *
 * @property itemId Identifier of item to drop
 * @property amount How many items should be dropped (from stack)
 * @property position Where this item should be dropped
 */
data class DropItemRequest(
    val itemId: Int,
    val amount: Int,
    val position: Position
): RequestPacket {

    constructor(data: ByteBuffer): this(
        itemId = data.getInt(),
        amount = data.getInt(),
        position = Position(
            x = data.getInt(),
            y = data.getInt(),
            z = data.getInt()
        )
    )

    init {
        require(amount > 0) { "Items amount should be more than zero" }
    }

}
