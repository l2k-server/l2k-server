package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.ArmorInstanceImpl
import org.l2kserver.game.model.item.ArrowInstanceImpl
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.JewelryInstanceImpl
import org.l2kserver.game.model.item.MagicItemInstanceImpl
import org.l2kserver.game.model.item.ItemInstanceImpl
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.BookInstanceImpl
import org.l2kserver.game.model.item.SoulshotInstanceImpl
import org.l2kserver.game.model.item.SpiritshotInstanceImpl
import org.l2kserver.game.model.item.template.Armor
import org.l2kserver.game.model.item.template.Arrow
import org.l2kserver.game.model.item.template.Book
import org.l2kserver.game.model.item.template.ItemTemplateRegistry
import org.l2kserver.game.model.item.template.Jewelry
import org.l2kserver.game.model.item.template.MagicItem
import org.l2kserver.game.model.item.template.Soulshot
import org.l2kserver.game.model.item.template.Spiritshot
import org.l2kserver.game.model.item.template.Weapon
import org.l2kserver.game.utils.IdUtils
import kotlin.Int

fun ItemEntity.toItemInstance(): ItemInstance? = when (val template = ItemTemplateRegistry.findById(this.templateId)) {
    is Weapon -> WeaponInstanceImpl(this, template)
    is Armor -> ArmorInstanceImpl(this, template)
    is Arrow -> ArrowInstanceImpl(this, template)
    is Jewelry -> JewelryInstanceImpl(this, template)
    is Soulshot -> SoulshotInstanceImpl(this, template)
    is Spiritshot -> SpiritshotInstanceImpl(this, template)
    is Book -> BookInstanceImpl(this, template)
    is MagicItem -> MagicItemInstanceImpl(this, template)
    else -> ItemInstanceImpl(this, template)
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
