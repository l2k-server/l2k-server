package org.l2kserver.game.model.store

import kotlinx.coroutines.sync.Mutex
import org.l2kserver.game.domain.item.template.Slot

private const val PRIVATE_STORE_SELL_ID = 1
private const val PRIVATE_STORE_BUY_ID = 3
// TODO https://github.com/l2kserver/l2kserver-game/issues/27 private const val PRIVATE_STORE_MANUFACTURE_ID = 5
private const val PRIVATE_STORE_PACKAGE_SALE_ID = 8

/**
 * PLayer's private store.
 *
 * @property title Title of store
 * @property storeType Store type identifier.
 */

sealed interface PrivateStore {
    val title: String
    val storeType: Int
    val mutex: Mutex

    data class Sell(
        override val title: String,
        val items: Map<Int, ItemOnSale>,
        val packageSale: Boolean
    ): PrivateStore {
        override val storeType: Int get() = if (packageSale) PRIVATE_STORE_PACKAGE_SALE_ID else PRIVATE_STORE_SELL_ID
        override val mutex = Mutex()

        constructor(title: String, items: List<ItemOnSale>, packageSale: Boolean): this(
            title = title,
            items = items.associateBy { it.itemId },
            packageSale = packageSale
        )
    }

    data class Buy(
        override val title: String,
        val items: List<ItemInWishList>,
    ): PrivateStore {

        override val storeType = PRIVATE_STORE_BUY_ID
        override val mutex = Mutex()
    }

}

/**
 * Item, registered in private store (Sell)
 *
 * @property itemId Identifier of item for sale
 * @property templateId Template ID of item for sale
 * @property categoryId ID of category of item for sale
 * @property amount Amount of items for sale
 * @property enchantLevel Enchant level of item for sale
 * @property recommendedPrice Price of item in server game data
 * @property price Item price in private store, set by player
 */
data class ItemOnSale(
    val itemId: Int,
    val templateId: Int,
    val categoryId: Int,
    val amount: Int,
    val enchantLevel: Int,
    val equippableAt: Slot?,
    val recommendedPrice: Int,
    val price: Int
) {
    init {
        require(amount >= 0) { "PrivateStore Item amount must be greater than 0!" }
        require(price >= 0) { "PrivateStore Item price must be greater or equal 0!" }
    }
}

/**
 * Item, registered in private store (buy)
 *
 * @property templateId Wished item template id
 * @property categoryId Wished item category id
 * @property amount Amount of items, that can be bought in this store
 * @property enchantLevel Wished item enchant level
 * @property equippableAt Wished item slot to equip
 * @property recommendedPrice Price of wished item in server game data
 * @property price Price of wished item, set by player
 */
data class ItemInWishList(
    val templateId: Int,
    val categoryId: Int,
    val amount: Int,
    val enchantLevel: Int,
    val equippableAt: Slot?,
    val recommendedPrice: Int,
    val price: Int
) {
    init {
        require(amount >= 0) { "PrivateStore Item amount must be greater than 0!" }
        require(price >= 0) { "PrivateStore Item price must be greater or equal 0!" }
    }
}

/**
 * Item suitable for private store, displayed in private store menu
 *
 * @property itemId ID of item
 * @property templateId template ID of the item
 * @property enchantLevel item enchant level
 * @property amount Amount of items
 * @property price Price of item in server game data
 * @property equippableAt slot to equip
 * @property categoryId item category id
 */
data class ItemInInventory(
    val itemId: Int,
    val templateId: Int,
    val enchantLevel: Int,
    val amount: Int,
    val price: Int,
    val equippableAt: Slot?,
    val categoryId: Int
)
