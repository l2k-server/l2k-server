package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val USE_ITEM_REQUEST_PACKET_ID: UByte = 20u

/**
 * Request to use item
 *
 * @property itemId Identifier of item to use
 */
data class UseItemRequest(
    val itemId: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(data.getInt())
}
