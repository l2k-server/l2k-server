package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.putUShort
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.model.store.ItemInInventory
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.ItemOnSale
import kotlin.collections.forEach

private const val ITEM_LIST_FOR_PRIVATE_STORE_BUY_RESPONSE_PACKET_ID: UByte = 183u
private const val SHOW_PRIVATE_STORE_BUY_RESPONSE_PACKET_ID: UByte = 184u
private const val SET_PRIVATE_STORE_BUY_MESSAGE_RESPONSE_PACKET_ID: UByte = 185u

/**
 * Sends data about items, that character can buy in private store (buy)
 *
 * @property characterId  ID of character, that intents to open private store
 * @property characterAdena Character's adena amount
 * @property itemsInInventory Items, that can be bought in private store
 * @property itemsInStore Already added to private store items
 */
data class ItemListForPrivateStoreBuyResponse(
    val characterId: Int,
    val characterAdena: Int,
    val itemsInInventory: Collection<ItemInInventory>,
    val itemsInStore: Collection<ItemInWishList>
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(ITEM_LIST_FOR_PRIVATE_STORE_BUY_RESPONSE_PACKET_ID)

        putInt(characterId)
        putInt(characterAdena)

        putInt(itemsInInventory.size)
        itemsInInventory.forEach { itemInInventory ->
            putInt(itemInInventory.templateId)
            putUShort(itemInInventory.enchantLevel.toUShort())
            putInt(itemInInventory.amount)
            putInt(itemInInventory.price)
            putShort(0)
            putInt(itemInInventory.equippableAt?.id ?: 0)
            putShort(itemInInventory.categoryId.toShort())
        }

        putInt(itemsInStore.size)
        itemsInStore.forEach { itemInStore ->
            putInt(itemInStore.templateId)
            putUShort(itemInStore.enchantLevel.toUShort())
            putInt(itemInStore.amount)
            putInt(itemInStore.price)
            putShort(0)
            putInt(itemInStore.equippableAt?.id ?: 0)
            putShort(itemInStore.categoryId.toShort())
            putInt(itemInStore.price)
            putInt(itemInStore.recommendedPrice)
        }
    }
}

/**
 * Shows private store (buy) window on client side
 *
 * @property ownerId Character, who created this store
 * @property customerAdena Adena of character
 * @property items Requested goods in this store
 */
data class ShowPrivateStoreBuyResponse(
    val ownerId: Int,
    val customerAdena: Int,
    val items: Collection<ItemOnSale>,
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SHOW_PRIVATE_STORE_BUY_RESPONSE_PACKET_ID)

        putInt(ownerId)
        putInt(customerAdena)

        putInt(items.size)
        items.forEach {
            putInt(it.itemId)
            putInt(it.templateId)
            putUShort(it.enchantLevel.toUShort())
            putInt(it.amount)

            putInt(it.recommendedPrice)
            putShort(0)

            putInt(it.equippableAt?.id ?: 0)
            putShort(it.categoryId.toShort())
            putInt(it.price)
            putInt(it.amount)
        }
    }

}

/**
 * Notify client about private store (sell) message update
 *
 * @property characterId ID of character, who opens private store (sell)
 * @property message New private store (sell) message
 */
data class PrivateStoreBuySetMessageResponse(
    val characterId: Int,
    val message: String
): ResponsePacket {
    override val data = littleEndianByteArray {
        putUByte(SET_PRIVATE_STORE_BUY_MESSAGE_RESPONSE_PACKET_ID)
        putInt(characterId)
        putUTF16String(message)
    }
}
