package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.BookTemplate
import org.l2kserver.game.model.item.template.PopupHintType
import org.l2kserver.game.model.item.template.ItemGroup

class Book(itemEntity: ItemEntity, itemTemplate: BookTemplate): ItemInstance {

    override val id = itemEntity.id.value
    override val templateId = itemEntity.templateId

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
    override val isStackable = itemTemplate.isStackable

    override val popUpHintType = PopupHintType.OTHER
    override val group = ItemGroup.ETC

    val text = itemTemplate.text
}
