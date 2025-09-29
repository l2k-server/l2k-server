package org.l2kserver.game.model.item.template

data class BookTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,

    val text: String
): ItemTemplate {
    override val isStackable = false
}
