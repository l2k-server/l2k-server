package org.l2kserver.game.extensions.model.item

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.ArmorInstanceImpl
import org.l2kserver.game.model.item.ArrowInstanceImpl
import org.l2kserver.game.model.item.ItemInstance
import org.l2kserver.game.model.item.JewelryInstanceImpl
import org.l2kserver.game.model.item.MagicItemInstanceImpl
import org.l2kserver.game.model.item.ItemInstanceImpl
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.BookInstanceImpl
import org.l2kserver.game.model.item.EnchantScrollInstanceImpl
import org.l2kserver.game.model.item.SoulshotInstanceImpl
import org.l2kserver.game.model.item.SpiritshotInstanceImpl
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.ArmorInstance
import org.l2kserver.game.model.item.Arrow
import org.l2kserver.game.model.item.Book
import org.l2kserver.game.model.item.EnchantScroll
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.model.item.JewelryInstance
import org.l2kserver.game.model.item.MagicItem
import org.l2kserver.game.model.item.Soulshot
import org.l2kserver.game.model.item.Spiritshot
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.model.item.WeaponInstance
import org.l2kserver.game.utils.IdUtils
import kotlin.Int

fun ItemEntity.toItemInstance() = when (val template = ItemRegistry.findById(this.templateId)) {
    is Weapon -> WeaponInstanceImpl(this, template)
    is Armor -> ArmorInstanceImpl(this, template)
    is Arrow -> ArrowInstanceImpl(this, template)
    is Jewelry -> JewelryInstanceImpl(this, template)
    is Soulshot -> SoulshotInstanceImpl(this, template)
    is Spiritshot -> SpiritshotInstanceImpl(this, template)
    is Book -> BookInstanceImpl(this, template)
    is MagicItem -> MagicItemInstanceImpl(this, template)
    is EnchantScroll -> EnchantScrollInstanceImpl(this, template)
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

fun ItemInstance.canBeEnchantedBy(scroll: EnchantScrollInstanceImpl): Boolean {
    if (this.grade != scroll.grade) return false

    return when (scroll.target) {
        EnchantScroll.Target.WEAPON -> this is WeaponInstance
        EnchantScroll.Target.ARMOR -> this is ArmorInstance || this is JewelryInstance
    }
}

val ItemInstance.canBeSold: Boolean get() = this.isEquipped || !this.isSellable
