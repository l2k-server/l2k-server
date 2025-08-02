package org.l2kserver.game.handler.dto.request

import org.l2kserver.game.model.item.Slot
import java.nio.ByteBuffer

const val DISARM_ITEM_REQUEST_PACKET_ID: UByte = 17u

/**
 * Request to take off item, equipped at [slot]
 */
data class TakeOffItemRequest(
    val slot: Slot
): RequestPacket {
    constructor(data: ByteBuffer): this(Slot.byId(data.getInt()))
}
