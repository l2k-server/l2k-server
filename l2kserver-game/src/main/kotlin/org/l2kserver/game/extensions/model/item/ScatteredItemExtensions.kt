package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.domain.item.template.Slot
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.Item

/**
 * Transforms [this] scattered item to item and places it to [ownerId]'s inventory
 *
 * @param ownerId New owner of this item
 * @param equippedAt Where should this item be equipped
 */
fun ScatteredItem.toItem(ownerId: Int, equippedAt: Slot? = null) = Item.create(
    templateId = this.templateId,
    ownerId = ownerId,
    amount = this.amount,
    equippedAt = equippedAt,
    enchantLevel = this.enchantLevel
)
