package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.putUShort
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.toInt
import org.l2kserver.game.model.store.ItemInInventory
import org.l2kserver.game.model.store.ItemOnSale
import kotlin.collections.forEach

private const val ITEM_LIST_FOR_PRIVATE_STORE_SELL_RESPONSE_PACKET_ID: UByte = 154u
private const val SHOW_PRIVATE_STORE_SELL_RESPONSE_PACKET_ID: UByte = 155u
private const val SET_PRIVATE_STORE_SELL_MESSAGE_RESPONSE_PACKET_ID: UByte = 156u

/**
 * Sends data about items, that character can sell in private store
 *
 * @property characterId  ID of character, that intents to open private store (for selling)
 * @property packageSale Should stackable items be sold in a full stack
 * @property characterAdena Character's adena amount
 * @property itemsInInventory Items, that can be sold in private store
 * @property itemsInStore Already added to private store items
 */
data class ItemListForPrivateStoreSellResponse(
    val characterId: Int,
    val packageSale: Boolean,
    val characterAdena: Int,
    val itemsInInventory: Collection<ItemInInventory>,
    val itemsInStore: Collection<ItemOnSale>
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ITEM_LIST_FOR_PRIVATE_STORE_SELL_RESPONSE_PACKET_ID)

        putInt(characterId)
        putInt(packageSale.toInt())
        putInt(characterAdena)

        putInt(itemsInInventory.size)
        itemsInInventory.forEach { itemInInventory ->
            putInt(itemInInventory.categoryId)
            putInt(itemInInventory.itemId)
            putInt(itemInInventory.templateId)
            putInt(itemInInventory.amount)

            putShort(0)
            putUShort(itemInInventory.enchantLevel.toUShort())
            putShort(0)

            putInt(itemInInventory.equippableAt?.id ?: 0)
            putInt(itemInInventory.price)
        }

        putInt(itemsInStore.size)
        itemsInStore.forEach { itemInStore ->
            putInt(itemInStore.categoryId)
            putInt(itemInStore.itemId)
            putInt(itemInStore.templateId)
            putInt(itemInStore.amount)

            putShort(0)
            putUShort(itemInStore.enchantLevel.toUShort())
            putShort(0)

            putInt(itemInStore.equippableAt?.id ?: 0)
            putInt(itemInStore.price)
            putInt(itemInStore.recommendedPrice)
        }
    }
}

/**
 * Shows private store (sell) window on client side
 *
 * @property ownerId Character, who created this store
 * @property buyerAdena Adena of character
 * @property items Goods in this store
 * @property packageSale Are the products sold in this store in a full stack
 */
data class ShowPrivateStoreSellResponse(
    val ownerId: Int,
    val buyerAdena: Int,
    val items: Collection<ItemOnSale>,
    val packageSale: Boolean
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SHOW_PRIVATE_STORE_SELL_RESPONSE_PACKET_ID)

        putInt(ownerId)
        putInt(packageSale.toInt())
        putInt(buyerAdena)

        putInt(items.size)
        items.forEach {
            putInt(it.categoryId)
            putInt(it.itemId)
            putInt(it.templateId)
            putInt(it.amount)

            putShort(0)
            putUShort(it.enchantLevel.toUShort())
            putShort(0)

            putInt(it.equippableAt?.id ?: 0)
            putInt(it.price)
            putInt(it.recommendedPrice)
        }
    }

}

/**
 * Notify client about private store (sell) message update
 *
 * @property characterId ID of character, who opens private store (sell)
 * @property message New private store (sell) message
 */
data class PrivateStoreSellSetMessageResponse(
    val characterId: Int,
    val message: String
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SET_PRIVATE_STORE_SELL_MESSAGE_RESPONSE_PACKET_ID)
        putInt(characterId)
        putUTF16String(message)
    }

}
