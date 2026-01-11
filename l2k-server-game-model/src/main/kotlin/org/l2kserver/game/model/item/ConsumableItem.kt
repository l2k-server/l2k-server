package org.l2kserver.game.model.item

import org.l2kserver.game.model.item.template.Item

/** Determines, which [amount] of item with [templateId] should be consumed by some action */
data class ConsumableItem(
    val templateId: Int,
    val amount: Int = 1
)

infix fun Int.of(item: Item) = ConsumableItem(item.id, this)
