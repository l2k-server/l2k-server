package org.l2kserver.game.model.item.template

abstract class ArrowTemplate: ItemTemplate {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean

    final override val isStackable = true
    final override val type = ArrowItemType
}

object ArrowItemType : ItemType {
    override val availableSlots = setOf(Slot.LEFT_HAND)
}
