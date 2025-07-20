package org.l2kserver.game.domain.item.entity

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.l2kserver.game.domain.item.template.Slot
import org.l2kserver.game.domain.postgresEnumeration

/**
 * Item data, stored at the database
 *
 * @property templateId id of item kind (for example Squire's Shirt's itemTemplateId is 1146)
 * @property ownerId Identifier of character, that owns this item
 * @property amount Quantity of items in stack
 * @property equippedAt Where is this item stored (in inventory). Null means this item is unequipped
 * @property enchantLevel Enchant level of this item (for armor, weapons and jewellery)
 * @property id Concrete item instance id
 */
object ItemTable: IntIdTable("items") {
    val templateId = integer("template_id")
    val ownerId = integer("owner_id")
    val amount = integer("amount")
    val equippedAt = postgresEnumeration<Slot>("equipped_at", "EQUIPPED_AT").nullable()
    val enchantLevel = integer("enchant_level")
    val augmentationId = integer("augmentation_id")
}

class ItemEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ItemEntity>(ItemTable)

    val templateId by ItemTable.templateId
    var ownerId by ItemTable.ownerId
    var amount by ItemTable.amount
    var equippedAt by ItemTable.equippedAt
    var enchantLevel by ItemTable.enchantLevel
    var augmentationId by ItemTable.augmentationId

}
