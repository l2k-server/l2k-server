package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val ENCHANT_ITEM_REQUEST_PACKET_ID: UByte = 88u

data class EnchantRequest(val itemId: Int): RequestPacket {

    constructor(data: ByteBuffer): this(
        itemId = data.getInt()
    )
}
