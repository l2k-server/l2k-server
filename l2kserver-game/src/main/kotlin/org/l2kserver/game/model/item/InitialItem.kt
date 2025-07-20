package org.l2kserver.game.model.item

/**
 * An item that must be given to character on creation
 *
 * @property id id of item kind (for example Squire's Shirt's item id is 1146)
 * @property name name of item
 * @property amount Range of item amount
 * @property isEquipped Is this item equipped (for initial items)
 * @property enchantLevel Enchant level of this item (for armor, weapons and jewellery)
 */
data class InitialItem(
    val id: Int,
    val name: String,
    val amount: Int = 1,
    val isEquipped: Boolean = false,
    val enchantLevel: Int = 0
)
