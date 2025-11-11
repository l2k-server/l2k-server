package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.Arrow
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.model.item.SimpleItem
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.Book
import org.l2kserver.game.model.item.Soulshot
import org.l2kserver.game.model.item.Spiritshot
import org.l2kserver.game.model.item.template.ArmorTemplate
import org.l2kserver.game.model.item.template.ArrowTemplate
import org.l2kserver.game.model.item.template.BookTemplate
import org.l2kserver.game.model.item.template.ItemTemplateRegistry
import org.l2kserver.game.model.item.template.JewelryTemplate
import org.l2kserver.game.model.item.template.SimpleItemTemplate
import org.l2kserver.game.model.item.template.SoulshotTemplate
import org.l2kserver.game.model.item.template.SpiritshotTemplate
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.utils.IdUtils
import kotlin.Int

fun ItemEntity.toItemInstance(): ItemInstance? = when (val template = ItemTemplateRegistry.findById(this.templateId)) {
    is WeaponTemplate -> Weapon(this, template)
    is ArmorTemplate -> Armor(this, template)
    is ArrowTemplate -> Arrow(this, template)
    is JewelryTemplate -> Jewelry(this, template)
    is SimpleItemTemplate -> SimpleItem(this, template)
    is SoulshotTemplate -> Soulshot(this, template)
    is SpiritshotTemplate -> Spiritshot(this, template)
    is BookTemplate -> Book(this, template)
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
