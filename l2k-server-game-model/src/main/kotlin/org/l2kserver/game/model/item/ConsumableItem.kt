package org.l2kserver.game.model.item

import org.l2kserver.game.model.item.template.ItemTemplate

/** Determines, which [amount] of item with [id] should be consumed by some action */
data class ConsumableItem(
    val id: Int,
    val amount: Int = 1
)

infix fun Int.of(item: ItemTemplate) = ConsumableItem(item.id, this)
