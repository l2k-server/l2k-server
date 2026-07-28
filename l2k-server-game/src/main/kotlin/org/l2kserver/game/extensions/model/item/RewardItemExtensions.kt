package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.reward.RewardItem

/**
 * Transforms this RewardItem to ScatteredItem
 *
 * @param position ScatteredItem's position in game world
 */
fun RewardItem.toScatteredItem(id: Int, position: Position, amount: Int): ScatteredItem? {
    val itemTemplate = ItemRegistry.findByIdOrNull(this.templateId) ?: run {
        System.err.println("No item template found by id ${this.templateId}")
        return null
    }

    return ScatteredItem(
        id = id,
        position = position,
        templateId = itemTemplate.id,
        isStackable = itemTemplate.isStackable,
        amount = amount,
        enchantLevel = this.enchantLevel
    )
}
