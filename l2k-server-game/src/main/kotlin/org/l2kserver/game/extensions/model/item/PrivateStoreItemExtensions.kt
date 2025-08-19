package org.l2kserver.game.extensions.model.item

import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.handler.dto.request.RequestedToBuyItem
import org.l2kserver.game.handler.dto.request.RequestedToSellItem
import org.l2kserver.game.handler.dto.request.RequestedToSellToPrivateStoreItem
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.ItemTemplate
import org.l2kserver.game.model.store.ItemInInventory
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.ItemOnSale
import kotlin.Int

/**
 * Finds requested item in [owner]'s inventory and checks if it can be requested to sell
 */
fun RequestedToSellItem.toItemInstance(owner: PlayerCharacter): ItemInstance = transaction {
    val requestedItem = this@toItemInstance
    val item = owner.inventory.findById(requestedItem.itemId)

    require(!item.isEquipped) { "Equipped item cannot be placed to private store!" }

    require(item.amount >= requestedItem.amount) { "Not enough $item to sell!" }
    require(item.isSellable) { "$owner is trying to sell non-sellable item in private store!" }

    item
}

/**
 * Convert private store item from request to ItemOnSale
 *
 * @param owner Current item owner
 */
fun RequestedToSellItem.toItemOnSale(owner: PlayerCharacter): ItemOnSale = transaction {
    val requestedItem = this@toItemOnSale
    val item = requestedItem.toItemInstance(owner)

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
    val itemTemplate = requireNotNull(ItemTemplate.Registry.findById(requestedItem.templateId)) {
        "No item template found by id ${requestedItem.templateId}" //TODO Nullable
    }
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

fun RequestedToSellToPrivateStoreItem.toItemInstance(owner: PlayerCharacter): ItemInstance = transaction {
    val requestedItem = this@toItemInstance
    val item = owner.inventory.findById(requestedItem.itemId)
    require(!item.isEquipped) { "Equipped item cannot be placed to private store!" }

    require(item.amount >= requestedItem.amount) { "Not enough $item to sell!" }
    require(item.isSellable) { "$owner is trying to sell non-sellable item in private store!" }

    item
}

fun ItemInstance.toItemInInventory(amount: Int = this.amount) = ItemInInventory(
    itemId = this.id,
    templateId = this.templateId,
    enchantLevel = this.enchantLevel,
    amount = amount,
    price = this.price,
    equippableAt = this.type.availableSlots.firstOrNull(),
    categoryId = this.category.id
)
