package org.l2kserver.game.extensions.model.item

import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.domain.item.template.ItemTemplate
import org.l2kserver.game.handler.dto.request.RequestedToBuyItem
import org.l2kserver.game.handler.dto.request.RequestedToSellItem
import org.l2kserver.game.handler.dto.request.RequestedToSellToPrivateStoreItem
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.store.ItemInInventory
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.ItemOnSale
import kotlin.Int

/**
 * Finds requested item in [ownerId]'s inventory and checks if it can be requested to sell
 */
fun RequestedToSellItem.toItem(ownerId: Int): Item = transaction {
    val requestedItem = this@toItem
    val item = Item.findById(requestedItem.itemId)
    require(!item.isEquipped) { "Equipped item cannot be placed to private store!" }

    require(item.amount >= requestedItem.amount) { "Not enough $item to sell!" }
    require(item.isSellable) { "Player '$ownerId' is trying to sell non-sellable item in private store!" }

    item
}

/**
 * Convert private store item from request to ItemOnSale
 *
 * @param ownerId Current item owner
 */
fun RequestedToSellItem.toItemOnSale(ownerId: Int): ItemOnSale = transaction {
    val requestedItem = this@toItemOnSale
    val item = requestedItem.toItem(ownerId)

    return@transaction ItemOnSale(
        itemId = item.id,
        templateId = item.templateId,
        categoryId = item.category.id,
        amount = requestedItem.amount,
        enchantLevel = item.enchantLevel,
        equippableAt = item.type.availableSlots.firstOrNull(),
        recommendedPrice = item.price,
        price = requestedItem.price
    )
}

fun RequestedToBuyItem.toItemInWishList(ownerId: Int): ItemInWishList = transaction {
    val requestedItem = this@toItemInWishList
    val itemTemplate = ItemTemplate.findById(requestedItem.templateId)
    require(itemTemplate.isSellable) { "Player '$ownerId' is trying to buy non-sellable item in private store (buy)!" }

    return@transaction ItemInWishList(
        templateId = itemTemplate.id,
        categoryId = itemTemplate.category.id,
        amount = requestedItem.amount,
        enchantLevel = requestedItem.enchantLevel,
        equippableAt = itemTemplate.type.availableSlots.firstOrNull(),
        recommendedPrice = itemTemplate.price,
        price = requestedItem.price
    )
}

fun RequestedToSellToPrivateStoreItem.toItem(ownerId: Int): Item = transaction {
    val requestedItem = this@toItem
    val item = Item.findById(requestedItem.itemId)
    require(!item.isEquipped) { "Equipped item cannot be placed to private store!" }

    require(item.amount >= requestedItem.amount) { "Not enough $item to sell!" }
    require(item.isSellable) { "Player '$ownerId' is trying to sell non-sellable item in private store!" }

    item
}

fun Item.toItemInInventory(amount: Int = this.amount) = ItemInInventory(
    itemId = this.id,
    templateId = this.templateId,
    enchantLevel = this.enchantLevel,
    amount = amount,
    price = this.price,
    equippableAt = this.type.availableSlots.firstOrNull(),
    categoryId = this.category.id
)
