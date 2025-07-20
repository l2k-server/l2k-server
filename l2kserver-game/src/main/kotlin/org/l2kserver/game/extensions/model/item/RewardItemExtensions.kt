package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.domain.item.template.ItemTemplate
import org.l2kserver.game.model.RewardItem
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.utils.IdUtils

/**
 * Transforms this RewardItem to ScatteredItem
 *
 * @param position ScatteredItem's position in game world
 */
fun RewardItem.toScatteredItem(position: Position, amount: Int): ScatteredItem {
    val itemTemplate = ItemTemplate.findById(this.id)

    return ScatteredItem(
        id = IdUtils.getNextScatteredItemId(),
        position = position,
        templateId = itemTemplate.id,
        isStackable = itemTemplate.isStackable,
        amount = amount,
        enchantLevel = this.enchantLevel
    )
}
