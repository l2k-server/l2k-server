package org.l2kserver.game.model.item.template

sealed interface Shot: Item

abstract class Soulshot: Shot {
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
}

enum class SpiritshotType {
    SPIRITSHOT,
    BLESSED_SPIRITSHOT
}

abstract class Spiritshot: Shot {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean
    abstract val spiritshotType: SpiritshotType

    final override val isStackable = true
}
