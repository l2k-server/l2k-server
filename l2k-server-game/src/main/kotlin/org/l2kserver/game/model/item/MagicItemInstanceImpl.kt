package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.MagicItemInstance
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.MagicItem
import org.l2kserver.game.model.item.template.PopupHintType
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import org.l2kserver.game.model.skill.ItemSkillInstanceImpl

/** Magic item instance (scroll, potion, etc.) that can be used to cast a skill */
class MagicItemInstanceImpl(itemEntity: ItemEntity, itemTemplate: MagicItem): MagicItemInstance {
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

    override val popUpHintType = PopupHintType.OTHER
    override val group = ItemGroup.ETC

    /** Item skill template that will be used when this item is used */
    private val skillTemplate = itemTemplate.skill

    override fun createSkill(characterId: Int): ActiveSkillInstance = ItemSkillInstanceImpl(skillTemplate, characterId)
}



