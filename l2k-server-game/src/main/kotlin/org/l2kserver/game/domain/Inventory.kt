package org.l2kserver.game.domain

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.extensions.model.item.toItemInstance
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.extensions.filterIsInstanceAnd
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.model.item.Weapon
import org.l2kserver.game.model.item.instance.EquippableItemInstance
import org.l2kserver.game.model.item.instance.ItemInstance
import org.l2kserver.game.model.item.template.ItemTemplate
import org.l2kserver.game.model.item.template.Slot
import java.util.EnumMap

private const val ADENA_TEMPLATE_ID = 57

/**
 * DAO class to access all the items of some character or pet
 */
class Inventory(val owner: PlayerCharacter): Collection<ItemInstance> {

    private lateinit var items: MutableMap<Int, ItemInstance>
    private lateinit var equippedItems: EnumMap<Slot, EquippableItemInstance>

    init {
        reload()
    }

    val twoSlotsAccessory: Jewelry? get() = equippedItems[Slot.TWO_SLOT_ACCESSORY] as Jewelry?
    val rightEarring: Jewelry? get() = equippedItems[Slot.RIGHT_EARRING] as Jewelry?
    val leftEarring: Jewelry? get() = equippedItems[Slot.LEFT_EARRING] as Jewelry?
    val necklace: Jewelry? get() = equippedItems[Slot.NECKLACE] as Jewelry?
    val rightRing: Jewelry? get() = equippedItems[Slot.RIGHT_RING] as Jewelry?
    val leftRing: Jewelry? get() = equippedItems[Slot.LEFT_RING] as Jewelry?
    val headgear: Armor? get() = equippedItems[Slot.HEADGEAR] as Armor?
    val oneHanded: Weapon? get() = equippedItems[Slot.RIGHT_HAND] as Weapon?
    val shield: Armor? get() = equippedItems[Slot.LEFT_HAND] as Armor?
    val gloves: Armor? get() = equippedItems[Slot.GLOVES] as Armor?
    val upperBody: Armor? get() = (equippedItems[Slot.UPPER_BODY]
            ?: equippedItems[Slot.UPPER_AND_LOWER_BODY]) as Armor?
    val lowerBody: Armor? get() = equippedItems[Slot.LOWER_BODY] as Armor?
    val boots: Armor? get() = equippedItems[Slot.BOOTS] as Armor?
    val underwear: Armor? get() = equippedItems[Slot.UNDERWEAR] as Armor?
    val twoHanded: Weapon? get() = equippedItems[Slot.TWO_HANDS] as Weapon?
    val faceAccessory: Jewelry? get() = equippedItems[Slot.FACE_ACCESSORY] as Jewelry?
    val hairAccessory: Jewelry? get() = equippedItems[Slot.HAIR_ACCESSORY] as Jewelry?

    /** Character's adena amount */
    val adena: ItemInstance? get() = findAllByTemplateId(ADENA_TEMPLATE_ID).firstOrNull()
    val weight: Int get() = items.values.sumOf { it.weight }
    val weapon: Weapon? get() = this.oneHanded ?: this.twoHanded

    operator fun get(key: Slot) = equippedItems[key]
    operator fun set(key: Slot, value: EquippableItemInstance?) {
        if (value != null) require(items.values.contains(value)) {
            "$owner tries to equip the item he does not own!!!"
        }
        equippedItems[key] = value
    }

    /** Creates new item at the inventory */
    fun createItem(templateId: Int, amount: Int = 1, equippedAt: Slot? = null, enchantLevel: Int = 0) = transaction {
        val itemTemplate = requireNotNull(ItemTemplate.Registry.findById(templateId)) {
            "Cannot add new item to the database - no template found by id=$templateId"
        }

        var item = ItemEntity.findAllByOwnerIdAndTemplateId(owner.id, templateId).firstOrNull()

        if (item == null || !itemTemplate.isStackable) item = ItemEntity.new {
            this.templateId = templateId
            this.ownerId = owner.id
            this.amount = amount
            this.equippedAt = equippedAt
            this.enchantLevel = enchantLevel
        }
        else {
            item.amount += amount
        }

        val instance = item.toItemInstance()!!

        items[instance.id] = instance
        if (instance.isEquipped) updateEquippedItems()

        return@transaction instance
    }

    /**
     * Finds item in this inventory by [itemId]
     *
     * @throws IllegalArgumentException if no item was found
     */
    fun findById(itemId: Int) = requireNotNull(items[itemId]) {
        "$owner has no item with id=$itemId in inventory"
    }

    /** Returns item with [templateId] in this inventory or null, if there is no such item */
    fun findAllByTemplateId(templateId: Int) = items.values.filter { it.templateId == templateId }

    /** Returns not equipped items by template ids */
    fun findAllNotEquippedByTemplateIds(templateIds: Iterable<Int>) = items.values.filter {
        it.equippedAt == null && templateIds.contains(it.templateId)
    }

    /** Returns item from this inventory by [itemId] or null, if it does not exist or is equipped */
    fun findNotEquippedByIdOrNull(itemId: Int) = items[itemId]?.takeIf { !it.isEquipped }

    /** Checks if item with [itemId] exists and its amount is greater or equal [amount] */
    fun existsByIdAndAmount(itemId: Int, amount: Int) = (items[itemId]?.amount ?: 0) >= amount

    /**
     * Reduces [ItemInstance.amount] on provided [value]. If [value] is equal to [ItemInstance.amount] - deletes item
     *
     * @throws IllegalArgumentException if [value] is greater than [ItemInstance.amount] or there is no item by [itemId] in this inventory
     * @return Updated item if amount was reduced, null if item was fully deleted
     */
    fun reduceAmount(itemId: Int, value: Int) = transaction {
        val item = requireNotNull(items[itemId]) { "No item with id $itemId exists at $owner inventory" }
        require(item.amount >= value) { "Cannot reduce $item amount - $value is greater than item amount" }

        return@transaction if (item.amount == value) {
            delete(item)
            null
        } else {
            item.amount -= value
            item
        }
    }

    /** Deletes item from inventory (and db) */
    fun delete(item: ItemInstance) = transaction {
        require(item.ownerId == owner.id) { "$owner tries to delete someone else's item!!!" }

        ItemTable.deleteWhere { ItemTable.id eq item.id }
        items.remove(item.id)

        if (item.isEquipped) updateEquippedItems()
    }

    /**
     * Equips this item - stores it to [slot] at paper doll and updates database
     *
     * @return equipped item
     */
    fun equipItem(item: EquippableItemInstance, slot: Slot): EquippableItemInstance = transaction {
        require(item.type.availableSlots.contains(slot)) { "$item cannot be equipped to $slot" }
        item.equippedAt = slot
        this@Inventory[slot] = item

        return@transaction item
    }

    /**
     * Disarms this item - replaces it from slot at paper doll and updates database
     *
     * @return disarmed item
     */
    fun disarmItem(item: EquippableItemInstance): EquippableItemInstance = transaction {
        item.equippedAt?.let {
            this@Inventory[it] = null
            item.equippedAt = null
        }
        return@transaction item
    }

    /** Reloads all the items from the database */
    fun reload() {
        updateItems()
        updateEquippedItems()
    }

    /** Reloads items from the database */
    private fun updateItems() = transaction {
        items = ItemEntity.findAllByOwnerId(owner.id)
            .mapNotNull { it.toItemInstance() }
            .associateBy { it.id }
            .toMutableMap()
    }

    /** Updates the equipped items list */
    private fun updateEquippedItems() {
        equippedItems = items.values
            .filterIsInstanceAnd<EquippableItemInstance> { it.equippedAt != null }
            .associateByTo(EnumMap(Slot::class.java)) { it.equippedAt!! }
    }

    override val size by items::size
    override fun contains(element: ItemInstance) = items.values.contains(element)
    override fun containsAll(elements: Collection<ItemInstance>) = items.values.containsAll(elements)
    override fun isEmpty() = items.isEmpty()
    override fun iterator() = items.values.iterator()
}
