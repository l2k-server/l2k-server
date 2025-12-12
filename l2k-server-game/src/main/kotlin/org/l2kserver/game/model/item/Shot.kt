package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.SoulshotInstance
import org.l2kserver.game.model.item.instance.SpiritshotInstance
import org.l2kserver.game.model.item.template.SoulshotTemplate
import org.l2kserver.game.model.item.template.SpiritshotTemplate

class Soulshot(itemEntity: ItemEntity, itemTemplate: SoulshotTemplate): SoulshotInstance {
    override val id: Int = itemEntity.id.value
    override val templateId = itemEntity.templateId

    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount

    override val name = itemTemplate.name
    override val type = itemTemplate.type
    override val grade = itemTemplate.grade
    override val weight = itemTemplate.weight
    override val price = itemTemplate.price
    override val isSellable = itemTemplate.isSellable
    override val isDroppable = itemTemplate.isDroppable
    override val isDestroyable = itemTemplate.isDestroyable
    override val isExchangeable = itemTemplate.isExchangeable

    override fun toString() = "Soulshot(name=$name id=$id amount=$amount grade=$grade)"
}

class Spiritshot(itemEntity: ItemEntity, itemTemplate: SpiritshotTemplate): SpiritshotInstance {
    override val id: Int = itemEntity.id.value
    override val templateId = itemEntity.templateId

    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount

    override val name = itemTemplate.name
    override val type = itemTemplate.type
    override val grade = itemTemplate.grade
    override val weight = itemTemplate.weight
    override val price = itemTemplate.price
    override val isSellable = itemTemplate.isSellable
    override val isDroppable = itemTemplate.isDroppable
    override val isDestroyable = itemTemplate.isDestroyable
    override val isExchangeable = itemTemplate.isExchangeable

    override val spiritshotType = itemTemplate.spiritshotType

    override fun toString() = "Spiritshot(name=$name id=$id amount=$amount type=$spiritshotType)"
}
