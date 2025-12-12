package org.l2kserver.game.model.item.instance

import org.l2kserver.game.model.item.Crystallizable
import org.l2kserver.game.model.item.Enchantable
import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.PopupHintType
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.CombatStats

interface ArmorInstance: EquippableItemInstance, Enchantable, Crystallizable {
    override val id: Int
    override val templateId: Int

    override var ownerId: Int
    override var amount: Int
    override var equippedAt: Slot?
    override var enchantLevel: Int

    override val name: String
    override val grade: Grade
    override val weight: Int
    override val price: Int
    override val isSellable: Boolean
    override val isDroppable: Boolean
    override val isDestroyable: Boolean
    override val isExchangeable: Boolean
    override val type: ArmorType
    override val crystalCount: Int

    override val popUpHintType get() = PopupHintType.ARMOR
    override val group get() = ItemGroup.ARMOR

    override val stats: CombatStats
    override val fixedBonusStats: CombatStats?
}
