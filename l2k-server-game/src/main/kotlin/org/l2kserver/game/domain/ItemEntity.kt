package org.l2kserver.game.domain

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.l2kserver.game.model.item.Slot

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
    val amount = integer("amount").default(1)
    val equippedAt = pgEnumeration<Slot>("equipped_at", "EQUIPPED_AT").nullable()
    val enchantLevel = integer("enchant_level").default(0)
    val augmentationId = integer("augmentation_id").default(0)
}

class ItemEntity(id: EntityID<Int>): IntEntity(id) {

    companion object: IntEntityClass<ItemEntity>(ItemTable) {

        /** Finds all the items owned by character with id=[ownerId] */
        fun findAllByOwnerId(ownerId: Int) = find { ItemTable.ownerId eq ownerId }

        /** Finds all the items with templateId=[templateId] of character with id=[ownerId] */
        fun findAllByOwnerIdAndTemplateId(ownerId: Int, templateId: Int) = find {
            (ItemTable.ownerId eq ownerId) and (ItemTable.templateId eq templateId)
        }

        /** Checks if item with id=[itemId] exist */
        fun existsById(itemId: Int) = count(ItemTable.id eq itemId) > 0

        /** Deletes all the items owned by character with id=[ownerId] */
        fun deleteAllByOwnerId(ownerId: Int) = ItemTable.deleteWhere { ItemTable.ownerId eq ownerId }
    }

    var templateId by ItemTable.templateId
    var ownerId by ItemTable.ownerId
    var amount by ItemTable.amount
    var equippedAt by ItemTable.equippedAt
    var enchantLevel by ItemTable.enchantLevel
    var augmentationId by ItemTable.augmentationId
}
