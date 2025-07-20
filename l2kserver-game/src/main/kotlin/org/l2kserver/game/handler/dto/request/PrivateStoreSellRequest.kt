package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUTF16String

const val ITEM_LIST_FOR_PRIVATE_STORE_SELL_REQUEST_PACKET_ID: UByte = 115u
const val START_PRIVATE_STORE_SELL_REQUEST_PACKET_ID: UByte = 116u
const val STOP_PRIVATE_STORE_SELL_REQUEST_PACKET_ID: UByte = 118u
const val SET_PRIVATE_STORE_SELL_MESSAGE_REQUEST_PACKET_ID: UByte = 119u
const val BUY_IN_PRIVATE_STORE_REQUEST_PACKET_ID: UByte = 121u

/**
 * Request to get list of items, suitable for selling in private store.
 * This packet is sent when player clicks "Private Store - Sell" action button
 */
data object ItemListForPrivateStoreSellRequest: RequestPacket

/**
 * Request to start private store (sell)
 *
 * @property packageSale If true, only full stacks of items can be sold
 * @property items Items to cell
 */
data class PrivateStoreSellStartRequest(
    val packageSale: Boolean,
    val items: List<RequestedToSellItem>
) : RequestPacket

fun PrivateStoreSellStartRequest(data: ByteBuffer): PrivateStoreSellStartRequest {
    val packageSell = data.getInt() == 1
    val slotsAmount = data.getInt()

    require(slotsAmount > 0) { "Private store (sell) slots amount must not be negative!" }

    val items: List<RequestedToSellItem> =
        if (slotsAmount <= 0 || slotsAmount * Int.SIZE_BYTES * 3 != data.remaining()) emptyList()
        else List(slotsAmount) {
            RequestedToSellItem(
                itemId = data.getInt(),
                amount = data.getInt(),
                price = data.getInt()
            )
        }

    return PrivateStoreSellStartRequest(packageSell, items)
}

/**
 * Request to stop private store (sell)
 */
data object PrivateStoreSellStopRequest: RequestPacket

/**
 * Request to change message of private store (sell)
 *
 * @property message New private store (sell) message
 */
data class PrivateStoreSellSetMessageRequest(val message: String): RequestPacket {
    constructor(data: ByteBuffer): this(
        message = data.getUTF16String()
    )
}

/**
 * Request to buy [items] in [storeOwnerId]'s private store (sell)
 */
data class BuyInPrivateStoreRequest(
    val storeOwnerId: Int,
    val items: List<RequestedToSellItem>
): RequestPacket

fun BuyInPrivateStoreRequest(data: ByteBuffer): BuyInPrivateStoreRequest {
    val sellerId = data.getInt()
    val itemsAmount = data.getInt()

    val items = List(itemsAmount) {
        RequestedToSellItem(
            itemId = data.getInt(),
            amount = data.getInt(),
            price = data.getInt()
        )
    }

    return BuyInPrivateStoreRequest(sellerId, items)
}

/**
 * Item to buy or sell in private store
 *
 * @property itemId Item to sell identifier
 * @property amount How many items should be sold
 * @property price Item price in private store
 */
data class RequestedToSellItem(
    val itemId: Int,
    val amount: Int,
    val price: Int
) {
    init {
        require(amount > 0) { "Amount of item in private store must be greater than 0!" }
        require(price >= 0) { "Price of item in private store must be greater or equal 0!" }
    }
}


