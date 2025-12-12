package org.l2kserver.game.model.item.instance

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.ItemType
import org.l2kserver.game.model.item.template.PopupHintType
import org.l2kserver.game.model.item.template.SpiritshotType

sealed interface ShotInstance: ItemInstance

interface SoulshotInstance: ShotInstance {
    override val id: Int
    override val templateId: Int

    override var ownerId: Int
    override var amount: Int

    override val name: String
    override val type: ItemType
    override val grade: Grade
    override val weight: Int
    override val price: Int
    override val isSellable: Boolean
    override val isDroppable: Boolean
    override val isDestroyable: Boolean
    override val isExchangeable: Boolean

    override val popUpHintType: PopupHintType get() = PopupHintType.OTHER
    override val group get() = ItemGroup.ETC
    override val isStackable: Boolean get() = true
}

interface SpiritshotInstance: ShotInstance {
    override val id: Int
    override val templateId: Int

    override var ownerId: Int
    override var amount: Int

    override val name: String
    override val type: ItemType
    override val grade: Grade
    override val weight: Int
    override val price: Int
    override val isSellable: Boolean
    override val isDroppable: Boolean
    override val isDestroyable: Boolean
    override val isExchangeable: Boolean

    override val popUpHintType: PopupHintType get() = PopupHintType.OTHER
    override val group get() = ItemGroup.ETC
    override val isStackable: Boolean get() = true

    val spiritshotType: SpiritshotType
}
