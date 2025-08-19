package org.l2kserver.game.extensions.model.store

import org.l2kserver.game.handler.dto.request.RequestedToSellItem
import org.l2kserver.game.handler.dto.request.RequestedToSellToPrivateStoreItem
import org.l2kserver.game.handler.dto.response.PrivateStoreBuySetMessageResponse
import org.l2kserver.game.handler.dto.response.PrivateStoreSellSetMessageResponse
import org.l2kserver.game.handler.dto.response.ResponsePacket
import org.l2kserver.game.handler.dto.response.ShowPrivateStoreBuyResponse
import org.l2kserver.game.handler.dto.response.ShowPrivateStoreSellResponse
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.PrivateStore
import org.l2kserver.game.model.store.ItemOnSale
import kotlin.Int

/**
 * Subtracts traded item from store list. If no traded items left in item slot - removes it.
 * If all the slots are empty now - returns null
 *
 * @return Updated private store or null, if no items left in store
 */
fun Map<Int, ItemOnSale>.subtractTradedItem(requestedToSellItem: RequestedToSellItem) = this[requestedToSellItem.itemId]?.let {
    val newItems = this.toMutableMap()

    val newAmount = it.amount - requestedToSellItem.amount

    if (newAmount <= 0) newItems.remove(requestedToSellItem.itemId)
    else newItems[requestedToSellItem.itemId] = it.copy(amount = newAmount)

    newItems as Map<Int, ItemOnSale>
} ?: this

/**
 * Subtracts traded item from store list. If no traded items left in item slot - removes it.
 * If all the slots are empty now - returns null
 *
 * @return Updated private store or null, if no items left in store
 */
fun List<ItemInWishList>.subtractTradedItem(requestedItem: RequestedToSellToPrivateStoreItem): List<ItemInWishList> {
    val updatedItems = ArrayList<ItemInWishList>()

    for (i: Int in this.indices) {
        val itemInWishList = this[i]
        val templateMatches = itemInWishList.templateId == requestedItem.templateId
        val enchantMatches = itemInWishList.enchantLevel == requestedItem.enchantLevel

        if (templateMatches && enchantMatches) {
            val newAmount = itemInWishList.amount - requestedItem.amount
            if (newAmount > 0) updatedItems.add(itemInWishList.copy(amount = newAmount))

            updatedItems += this.subList(i + 1, this.size)
            break
        }
        else updatedItems.add(itemInWishList)
    }

    return updatedItems as List<ItemInWishList>
}

fun PrivateStore.toInfoResponse(storeOwner: PlayerCharacter, storeCustomer: PlayerCharacter): ResponsePacket =
    when (this) {
        is PrivateStore.Sell -> ShowPrivateStoreSellResponse(
            ownerId = storeOwner.id,
            buyerAdena = storeCustomer.inventory.adena?.amount ?: 0,
            items = this.items.values,
            packageSale = this.packageSale
        )

        is PrivateStore.Buy -> {
            //Find all the items, which customer can sell to private store
            val customerItems = storeCustomer.inventory
                .findAllNotEquippedByTemplateIds(this.items.map { it.templateId })
                .toMutableSet()

            ShowPrivateStoreBuyResponse(
                ownerId = storeOwner.id,
                customerAdena = storeCustomer.inventory.adena?.amount ?: 0,
                items = this.items.map { itemInWishList ->
                    // Find customer item in customer items set
                    val customerItem = customerItems.find {
                        it.templateId == itemInWishList.templateId && it.enchantLevel == itemInWishList.enchantLevel
                    }

                    // If found - delete this item. If Owner is buying several non-stackable items,
                    // this will make client to show sellable only such amount of items, that customer has
                    customerItem?.let { customerItems.remove(it) }
                    ItemOnSale(
                        itemId = customerItem?.id ?: 0,
                        templateId = itemInWishList.templateId,
                        categoryId = itemInWishList.categoryId,
                        amount = minOf(customerItem?.amount ?: 0, itemInWishList.amount),
                        enchantLevel = itemInWishList.enchantLevel,
                        equippableAt = itemInWishList.equippableAt,
                        recommendedPrice = itemInWishList.recommendedPrice,
                        price = itemInWishList.price
                    )
                }
            )
        }
    }

fun PrivateStore.toMessageResponse(ownerId: Int) = when (this) {
    is PrivateStore.Sell -> PrivateStoreSellSetMessageResponse(ownerId, this.title)
    is PrivateStore.Buy -> PrivateStoreBuySetMessageResponse(ownerId, this.title)
}
