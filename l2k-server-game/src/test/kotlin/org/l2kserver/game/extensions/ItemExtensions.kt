package org.l2kserver.game.extensions

import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.store.ItemInWishList
import org.l2kserver.game.model.store.ItemOnSale

fun Item.toItemOnSale(price: Int, amount: Int = this.amount): ItemOnSale {
    require(amount <= this.amount) { "Not enough items to map it to ItemOnSale!" }

    return ItemOnSale(
        itemId = this.id,
        templateId = this.templateId,
        categoryId = this.category.id,
        amount = amount,
        enchantLevel = this.enchantLevel,
        equippableAt = this.type.availableSlots.firstOrNull(),
        recommendedPrice = this.price,
        price = price
    )
}

fun Item.toItemInWishList(price: Int, amount: Int = this.amount) = ItemInWishList(
    templateId = this.templateId,
    categoryId = this.category.id,
    amount = amount,
    enchantLevel = this.enchantLevel,
    equippableAt = this.type.availableSlots.firstOrNull(),
    recommendedPrice = this.price,
    price = price
)
