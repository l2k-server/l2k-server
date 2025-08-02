package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.ItemTemplate
import org.l2kserver.game.model.reward.RewardItem
import org.l2kserver.game.utils.IdUtils

/**
 * Transforms this RewardItem to ScatteredItem
 *
 * @param position ScatteredItem's position in game world
 */
fun RewardItem.toScatteredItem(position: Position, amount: Int): ScatteredItem? {
    val itemTemplate = ItemTemplate.Registry.findById(this.id) ?: run {
        System.err.println("No item template found by id ${this.id}")
        return null
    }

    return ScatteredItem(
        id = IdUtils.getNextScatteredItemId(),
        position = position,
        templateId = itemTemplate.id,
        isStackable = itemTemplate.isStackable,
        amount = amount,
        enchantLevel = this.enchantLevel
    )
}
