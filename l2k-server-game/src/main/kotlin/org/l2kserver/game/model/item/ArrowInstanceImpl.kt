package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.Arrow
import org.l2kserver.game.model.item.template.ItemGroup

class ArrowInstanceImpl(itemEntity: ItemEntity, itemTemplate: Arrow): ItemInstance {
    override val id: Int = itemEntity.id.value
    override val templateId = itemEntity.templateId

    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount
    override var equippedAt by itemEntity::equippedAt

    override val name = itemTemplate.name
    override val type = itemTemplate.type
    override val grade = itemTemplate.grade
    override val weight = itemTemplate.weight
    override val price = itemTemplate.price
    override val isSellable = itemTemplate.isSellable
    override val isDroppable = itemTemplate.isDroppable
    override val isDestroyable = itemTemplate.isDestroyable
    override val isExchangeable = itemTemplate.isExchangeable
    override val isStackable = true

    override val popUpHintType = itemTemplate.popupHintType
    override val group = ItemGroup.ETC

    override fun toString() = "Arrow(name=$name id=$id amount=$amount)"

}
