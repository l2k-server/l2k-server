package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.Arrow
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.model.item.SimpleItemInstance
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.template.ArmorTemplate
import org.l2kserver.game.model.item.template.ArrowTemplate
import org.l2kserver.game.model.item.template.ItemTemplate
import org.l2kserver.game.model.item.template.JewelryTemplate
import org.l2kserver.game.model.item.template.SimpleItemTemplate
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.utils.IdUtils
import kotlin.Int

fun ItemEntity.toItemInstance(): ItemInstance? = when (val itemTemplate = ItemTemplate.Registry.findById(this.templateId)) {
    is WeaponTemplate -> Weapon(this, itemTemplate)
    is ArmorTemplate -> Armor(this, itemTemplate)
    is ArrowTemplate -> Arrow(this, itemTemplate)
    is JewelryTemplate -> Jewelry(this, itemTemplate)
    is SimpleItemTemplate -> SimpleItemInstance(this, itemTemplate)
    else -> null
}

fun ItemInstance.toScatteredItem(position: Position, amount: Int) = ScatteredItem(
    //ID must be new, otherwise client fails displaying picking up this scattered item
    id = IdUtils.getNextScatteredItemId(),
    position = position,
    templateId = this.templateId,
    isStackable = this.isStackable,
    amount = amount,
    enchantLevel = this.enchantLevel
)

/**
 * Transforms [this] scattered item to item and places it to [owner]'s inventory
 *
 * @param owner New owner of this item
 * @param equippedAt Where should this item be equipped
 */
fun ScatteredItem.toItemInstance(owner: PlayerCharacter, equippedAt: Slot? = null) = owner.inventory.createItem(
    this.templateId, this.amount, equippedAt, this.enchantLevel
)
