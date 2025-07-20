package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.domain.item.template.Slot

const val DISARM_ITEM_REQUEST_PACKET_ID: UByte = 17u

/**
 * Request to take off item, equipped at [slot]
 */
data class TakeOffItemRequest(
    val slot: Slot
): RequestPacket {
    constructor(data: ByteBuffer): this(Slot.byId(data.getInt()))
}
