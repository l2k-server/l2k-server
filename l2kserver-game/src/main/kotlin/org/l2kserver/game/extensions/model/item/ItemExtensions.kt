package org.l2kserver.game.extensions.model.item

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.actor.ScatteredItem
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.domain.item.template.ArmorTemplate
import org.l2kserver.game.model.item.Arrow
import org.l2kserver.game.domain.item.template.ArrowTemplate
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.domain.item.template.ItemTemplate
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.domain.item.template.JewelryTemplate
import org.l2kserver.game.model.item.InitialItem
import org.l2kserver.game.model.item.SimpleItem
import org.l2kserver.game.domain.item.template.SimpleItemTemplate
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.domain.item.template.WeaponTemplate
import org.l2kserver.game.domain.item.entity.ItemEntity
import org.l2kserver.game.domain.item.entity.ItemTable
import org.l2kserver.game.domain.item.template.Slot
import org.l2kserver.game.utils.IdUtils
import kotlin.Int

const val ADENA_TEMPLATE_ID = 57

fun ItemEntity.toItem(): Item = when (val itemTemplate = ItemTemplate.findById(this.templateId)) {
    is WeaponTemplate -> Weapon(this, itemTemplate)
    is ArmorTemplate -> Armor(this, itemTemplate)
    is ArrowTemplate -> Arrow(this, itemTemplate)
    is JewelryTemplate -> Jewelry(this, itemTemplate)
    is SimpleItemTemplate -> SimpleItem(this, itemTemplate)
}

fun Item.toScatteredItem(position: Position, amount: Int) = ScatteredItem(
    //ID must be new, otherwise client fails displaying picking up this scattered item
    id = IdUtils.getNextScatteredItemId(),
    position = position,
    templateId = this.templateId,
    isStackable = this.isStackable,
    amount = amount,
    enchantLevel = this.enchantLevel
)

/**
 * Deletes this item from database
 */
fun Item.delete() = transaction {
    ItemTable.deleteWhere { id eq this@delete.id }
    return@transaction this@delete
}

/**
 * Creates [this] item copy at [newOwnerId]'s inventory
 */
fun Item.copyTo(newOwnerId: Int, amount: Int = this.amount, equippedAt: Slot? = null) = transaction {
    val oldItem = this@copyTo

    Item.create(
        templateId = oldItem.templateId,
        ownerId = newOwnerId,
        amount = amount,
        equippedAt = equippedAt,
        enchantLevel = enchantLevel
    )
}

/**
 * Reduces [Item.amount] on provided [value]. If [value] is equal to [Item.amount] - deletes item
 *
 * @throws IllegalArgumentException if [value] is greater than [Item.amount]
 * @return Updated item if amount was reduced, null if item was fully deleted
 */
fun Item.reduceAmountBy(value: Int) = transaction {
    val item = this@reduceAmountBy
    require(item.amount >= value) { "Cannot reduce $item amount - $value is greater than item amount" }

    return@transaction if (item.amount == value) {
        item.delete()
        null
    } else {
        item.amount -= value
        item
    }
}

/**
 * Creates new item
 *
 * @param templateId Template ID of new item
 * @param ownerId New item owner
 * @param amount Item amount in stack
 * @param equippedAt Where is this item equipped
 * @param enchantLevel this item enchant level
 */
fun Item.Companion.create(
    templateId: Int,
    ownerId: Int,
    amount: Int = 1,
    equippedAt: Slot? = null,
    enchantLevel: Int = 0
) = transaction {
    ItemTable.insertReturning { statement ->
        statement[ItemTable.templateId] = templateId
        statement[ItemTable.ownerId] = ownerId
        statement[ItemTable.amount] = amount
        statement[ItemTable.equippedAt] = equippedAt
        statement[ItemTable.enchantLevel] = enchantLevel
        statement[augmentationId] = 0
    }.map { ItemEntity.wrapRow(it).toItem() }.first()
}

/**
 * Creates new items from provided [initialItems] and assigns it to owner with given [ownerId]. Saves new items to DB
 */
fun Item.Companion.createAllFrom(ownerId: Int, initialItems: List<InitialItem>) = initialItems.map {
    create(
        templateId = it.id,
        ownerId = ownerId,
        amount = it.amount,
        equippedAt = if (it.isEquipped)
            ItemTemplate.findById(it.id).type.availableSlots.firstOrNull() else null,
        enchantLevel = it.enchantLevel
    )
}

fun Item.Companion.findAllByOwnerIdAndTemplateId(
    ownerId: Int, templateId: Int, withLock: Boolean = false
) = findAllByOwnerIdAndTemplateIds(ownerId, listOf(templateId), withLock)

/**
 * Finds all the items that belong to [ownerId] and have templateId in [templateIds]
 *
 * @param withLock - Should the item records be locked for current transaction
 */
fun Item.Companion.findAllByOwnerIdAndTemplateIds(
    ownerId: Int, templateIds: Iterable<Int>, withLock: Boolean = false
) = transaction {
    val query = ItemTable.selectAll()
        .where { (ItemTable.ownerId eq ownerId) and (ItemTable.templateId inList templateIds) }

    if (withLock) query.forUpdate()

    query.map { ItemEntity.wrapRow(it).toItem() }
}

fun Item.Companion.findAllByOwnerId(ownerId: Int) = ItemEntity
    .find { ItemTable.ownerId eq ownerId }
    .map { it.toItem() }

fun Item.Companion.findAllEquippedByOwnerId(ownerId: Int) = ItemEntity
    .find { (ItemTable.ownerId eq ownerId) and (ItemTable.equippedAt neq null) }
    .map { it.toItem() }

fun Item.Companion.findAllNotEquippedByOwnerId(ownerId: Int) = ItemEntity
    .find { (ItemTable.ownerId eq ownerId) and (ItemTable.equippedAt eq null) }
    .map { it.toItem() }

fun Item.Companion.findAllNotEquippedByOwnerIdAndTemplateIds(
    ownerId: Int, templateIds: Iterable<Int>
) = transaction {
    ItemEntity.find {
        (ItemTable.ownerId eq ownerId) and (ItemTable.templateId inList templateIds) and (ItemTable.equippedAt eq null)
    }.map { it.toItem() }
}

/**
 * Calculates weight of all the items, carried by [ownerId]
 */
fun Item.Companion.countWeightByOwnerId(ownerId: Int) = ItemTable
    .select(ItemTable.templateId, ItemTable.amount).where { ItemTable.ownerId eq ownerId }
    .sumOf { ItemTemplate.findById(it[ItemTable.templateId]).weight * it[ItemTable.amount] }

/**
 * Checks if item with [itemId] exists in database
 */
fun Item.Companion.existsById(itemId: Int) = transaction {
    ItemEntity.count(ItemTable.id eq itemId) > 0
}

/**
 * Checks if item with [itemId] exists in [ownerId]'s inventory, and it's amount is greater or equal [amount]
 */
fun Item.Companion.existsByIdAndAmountAndOwnerId(itemId: Int, amount: Int, ownerId: Int) = transaction {
    ItemEntity.count(
        (ItemTable.id eq itemId) and (ItemTable.amount greaterEq amount) and (ItemTable.ownerId eq ownerId)
    ) > 0
}

/**
 * Finds item by [itemId] and [ownerId]
 */
fun Item.Companion.findNotEquippedByIdAndOwnerIdOrNull(
    itemId: Int, ownerId: Int
) = transaction {
    ItemTable.selectAll()
        .where { (ItemTable.id eq itemId) and (ItemTable.ownerId eq ownerId) and (ItemTable.equippedAt eq null) }
        .map { ItemEntity.wrapRow(it).toItem() }
        .firstOrNull()
}

/**
 * Finds adena item of [characterId]
 *
 * @param withLock if true - locks adena for read until transaction ends. By default - true
 */
fun Item.Companion.findCharacterAdena(characterId: Int, withLock: Boolean = true) = Item
    .findAllByOwnerIdAndTemplateId(characterId, ADENA_TEMPLATE_ID, withLock).firstOrNull()
