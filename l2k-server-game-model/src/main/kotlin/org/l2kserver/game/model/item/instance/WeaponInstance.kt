package org.l2kserver.game.model.item.instance

import org.l2kserver.game.model.item.Augmentable
import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.Crystallizable
import org.l2kserver.game.model.item.Enchantable
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.PopupHintType
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.stats.CombatStats

interface WeaponInstance: EquippableItemInstance, Enchantable, Augmentable, Crystallizable {
    override val id: Int
    override val templateId: Int

    override var ownerId: Int
    override var amount: Int
    override var equippedAt: Slot?
    override var enchantLevel: Int
    override var augmentationId: Int
    override val name: String
    override val grade: Grade
    override val weight: Int
    override val price: Int
    override val isSellable: Boolean
    override val isDroppable: Boolean
    override val isDestroyable: Boolean
    override val isExchangeable: Boolean
    override val type: WeaponType
    override val crystalCount: Int

    val soulshotUsed: Int
    val spiritshotUsed: Int

    var soulshotCharged: Boolean
    var spiritshotChargedType: SpiritshotType?

    val manaCost: Int
    val consumes: ConsumableItem?

    override val popUpHintType get() = PopupHintType.WEAPON
    override val group get() = ItemGroup.WEAPON_OR_JEWELRY

    override val fixedBonusStats: CombatStats?
}
