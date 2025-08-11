package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val DELETE_ITEM_REQUEST_PACKET_ID: UByte = 89u

/**
 * Request to delete item
 *
 * @property itemId Identifier of item to delete
 * @property amount How many items should be deleted
 */
data class DeleteItemRequest(
    val itemId: Int,
    val amount: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(data.getInt(), data.getInt())

    init {
        require(amount > 0) { "Items amount should be more than zero" }
    }

}
