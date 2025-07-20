package org.l2kserver.game.domain.item.template

import com.fasterxml.jackson.annotation.JsonRootName

/**
 * Template of a simple item, that cannot be consumed of equipped (for example materials or adena)
 */
@JsonRootName("item")
data class SimpleItemTemplate(
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
    override val type = SimpleItemType
    override val category = ItemCategory.OTHER //TODO Money??
}

object SimpleItemType : ItemType {
    override val availableSlots: Set<Slot> = emptySet()
}
