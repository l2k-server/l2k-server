package org.l2kserver.game.domain.item.template

import com.fasterxml.jackson.annotation.JsonRootName

@JsonRootName("arrow")
data class ArrowTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,
    override val isStackable: Boolean
): ItemTemplate {
    override val type = ArrowItemType
    override val category = ItemCategory.OTHER
}

object ArrowItemType : ItemType {
    override val availableSlots = setOf(Slot.LEFT_HAND)
}
