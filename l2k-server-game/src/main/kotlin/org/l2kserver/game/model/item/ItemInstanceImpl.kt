package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.skill.ItemSkillInstanceImpl
import org.l2kserver.game.model.skill.instance.ActiveSkillInstance

open class ItemInstanceImpl(entity: ItemEntity, template: Item): ItemInstance {
    override val id: Int = entity.id.value
    override val templateId = entity.templateId

    override var ownerId by entity::ownerId
    override var amount by entity::amount
    override val equippedAt: Slot? = null
    override var enchantLevel by entity::enchantLevel
    override val augmentationId by entity::augmentationId

    override val name = template.name
    override val type = template.type
    override val grade = template.grade
    override val weight = template.weight
    override val price = template.price
    override val isSellable = template.isSellable
    override val isDroppable = template.isDroppable
    override val isDestroyable = template.isDestroyable
    override val isExchangeable = template.isExchangeable
    override val isStackable = template.isStackable

    override val popUpHintType = template.popupHintType
    override val group = template.group

    override fun toString() = "Item(name=$name id=$id amount=$amount)"
}

sealed class EquippableItemInstanceImpl(
    entity: ItemEntity, template: Item
): ItemInstanceImpl(entity, template), EquippableItemInstance {
    override var equippedAt by entity::equippedAt

    final override val isStackable = false
}

class ArrowInstanceImpl(
    entity: ItemEntity, template: Arrow
): ItemInstanceImpl(entity, template) {
    override var equippedAt by entity::equippedAt
    override fun toString() = "Arrow(name=$name id=$id amount=$amount)"
}

class BookInstanceImpl(
    entity: ItemEntity, template: Book
): ItemInstanceImpl(entity, template) {
    val text = template.text
}

class EnchantScrollInstanceImpl(
    entity: ItemEntity, template: EnchantScroll
): ItemInstanceImpl(entity, template) {
    val target = template.target
    val isBlessed = template.isBlessed
}

/** Magic item instance (scroll, potion, etc.) that can be used to cast a skill */
class MagicItemInstanceImpl(
    entity: ItemEntity, template: MagicItem
): MagicItemInstance, ItemInstanceImpl(entity, template) {

    /** Item skill template that will be used when this item is used */
    private val skillTemplate = template.skill

    override fun createSkill(characterId: Int): ActiveSkillInstance = ItemSkillInstanceImpl(skillTemplate, characterId)
}

class SoulshotInstanceImpl(
    entity: ItemEntity, template: Soulshot
): SoulshotInstance, ItemInstanceImpl(entity, template) {
    override fun toString() = "Soulshot(name=$name id=$id amount=$amount grade=$grade)"
}

class SpiritshotInstanceImpl(
    entity: ItemEntity, template: Spiritshot
): SpiritshotInstance, ItemInstanceImpl(entity, template) {
    override val spiritshotType = template.spiritshotType

    override fun toString() = "Spiritshot(name=$name id=$id amount=$amount type=$spiritshotType)"
}
