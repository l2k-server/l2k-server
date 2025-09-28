package org.l2kserver.game.model.item.template

data class SoulshotTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean
): ItemTemplate {
    override val isStackable = true
}

enum class SpiritshotType {
    SPIRITSHOT,
    BLESSED_SPIRITSHOT
}

data class SpiritshotTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,
    val spiritshotType: SpiritshotType
): ItemTemplate {
    override val isStackable = true
}
