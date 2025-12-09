package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.ItemTemplate

class SimpleItem(
    private val itemEntity: ItemEntity,
    itemTemplate: ItemTemplate
): ItemInstance {
    override val id: Int = itemEntity.id.value

    override val templateId by itemEntity::templateId
    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount

    override val name = itemTemplate.name
    override val type = itemTemplate.type
    override val weight = itemTemplate.weight
    override val price = itemTemplate.price
    override val isSellable = itemTemplate.isSellable
    override val isDroppable = itemTemplate.isDroppable
    override val isDestroyable = itemTemplate.isDestroyable
    override val isExchangeable = itemTemplate.isExchangeable
    override val isStackable = true

    override val popUpHintType = itemTemplate.popupHintType
    override val group = ItemGroup.ETC

    override fun toString() = "Item(name=$name id=$id amount=$amount)"
}
