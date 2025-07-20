package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUTF16String

const val ITEM_LIST_FOR_PRIVATE_STORE_BUY_REQUEST_PACKET_ID: UByte = 144u
const val START_PRIVATE_STORE_BUY_REQUEST_PACKET_ID: UByte = 145u
const val STOP_PRIVATE_STORE_BUY_REQUEST_PACKET_ID: UByte = 147u
const val SET_PRIVATE_STORE_BUY_MESSAGE_REQUEST_PACKET_ID: UByte = 148u
const val SELL_TO_PRIVATE_STORE_BUY_REQUEST_PACKET_ID: UByte = 150u

/**
 * Request to get items, that can be placed to private store (buy)
 */
data object ItemListForPrivateStoreBuyRequest: RequestPacket

/**
 * Request to start handling private store (sell)
 *
 * @property items Items, requested to place to private store (buy)
 */
data class PrivateStoreBuyStartRequest(
    val items: List<RequestedToBuyItem>
): RequestPacket

fun PrivateStoreBuyStartRequest(data: ByteBuffer): PrivateStoreBuyStartRequest {
    val size = data.getInt()

    return PrivateStoreBuyStartRequest(
        items = List(size) {
            RequestedToBuyItem(
                templateId = data.getInt(),
                enchantLevel = data.getInt(),
                amount = data.getInt(),
                price = data.getInt()
            )
        }
    )
}

/**
 * Request to stop private store (sell)
 */
data object PrivateStoreBuyStopRequest : RequestPacket

/**
 * Request to change message of private store (sell)
 *
 * @property message New private store (sell) message
 */
data class PrivateStoreBuySetMessageRequest(val message: String) : RequestPacket {
    constructor(data: ByteBuffer) : this(
        message = data.getUTF16String()
    )
}

/**
 * Request to sell [items] to [storeOwnerId]'s private store (buy)
 */
data class SellToPrivateStoreRequest(
    val storeOwnerId: Int,
    val items: List<RequestedToSellToPrivateStoreItem>
): RequestPacket

fun SellToPrivateStoreRequest(data: ByteBuffer): SellToPrivateStoreRequest {
    val ownerId = data.getInt()

    val itemSize = data.getInt()
    val items = List(itemSize) {
        RequestedToSellToPrivateStoreItem(
            itemId = data.getInt(),
            templateId = data.getInt(),
            enchantLevel = data.getInt(),
            amount = data.getInt(),
            price = data.getInt()
        )
    }

    return SellToPrivateStoreRequest(ownerId, items)
}

/**
 * Item, requested to buy in private store (buy)
 */
data class RequestedToBuyItem(
    val templateId: Int,
    val enchantLevel: Int,
    val amount: Int,
    val price: Int
) {
    init {
        require(amount > 0) { "Amount of item in private store must be greater than 0!" }
        require(price >= 0) { "Price of item in private store must be greater or equal 0!" }
    }
}

data class RequestedToSellToPrivateStoreItem(
    val itemId: Int,
    val templateId: Int,
    val enchantLevel: Int,
    val amount: Int,
    val price: Int
) {
    init {
        require(amount > 0) { "Amount of item in private store must be greater than 0!" }
        require(price >= 0) { "Price of item in private store must be greater or equal 0!" }
    }
}
