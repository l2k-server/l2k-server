package org.l2kserver.game.model.item.template

abstract class Book: Item {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean
    abstract val text: String

    final override val isStackable = false
}
