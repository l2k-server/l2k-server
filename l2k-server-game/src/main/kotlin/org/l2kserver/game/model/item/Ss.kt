package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.SoulshotTemplate
import org.l2kserver.game.model.item.template.SpiritshotTemplate

sealed interface Ss {
    val id: Int
    val grade: Grade
}

class Soulshot(
    private val itemEntity: ItemEntity,
    itemTemplate: SoulshotTemplate
): ItemInstance, Ss {
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
    override val isStackable = itemTemplate.isStackable

    override val category = itemTemplate.category
    override val group = ItemGroup.ETC
}

class Spiritshot(
    private val itemEntity: ItemEntity,
    itemTemplate: SpiritshotTemplate
): ItemInstance, Ss {
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
    override val isStackable = itemTemplate.isStackable

    override val category = itemTemplate.category
    override val group = ItemGroup.ETC

    val isBlessed = itemTemplate.isBlessed
}
