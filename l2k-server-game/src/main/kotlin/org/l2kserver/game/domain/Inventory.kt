package org.l2kserver.game.domain

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.l2kserver.game.extensions.model.item.toItemInstance
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.extensions.filterIsInstanceAnd
import org.l2kserver.game.model.item.ArmorInstanceImpl
import org.l2kserver.game.model.item.JewelryInstanceImpl
import org.l2kserver.game.model.item.EquippableItemInstanceImpl
import org.l2kserver.game.model.item.WeaponInstanceImpl
import org.l2kserver.game.model.item.ItemInstance
import org.l2kserver.game.model.item.ItemInstanceImpl
import org.l2kserver.game.model.item.ItemRegistry
import org.l2kserver.game.model.item.Slot
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap

private const val ADENA_TEMPLATE_ID = 57

/**
 * DAO class to access all the items of character
 */
class Inventory(val owner: PlayerCharacterInstanceImpl): Collection<ItemInstance> {

    private val items: MutableMap<Int, ItemInstanceImpl> = ConcurrentHashMap()
    private lateinit var equippedItems: EnumMap<Slot, EquippableItemInstanceImpl>

    init {
        reload()
    }

    val twoSlotsAccessory: JewelryInstanceImpl? get() = equippedItems[Slot.TWO_SLOT_ACCESSORY] as JewelryInstanceImpl?
    val rightEarring: JewelryInstanceImpl? get() = equippedItems[Slot.RIGHT_EARRING] as JewelryInstanceImpl?
    val leftEarring: JewelryInstanceImpl? get() = equippedItems[Slot.LEFT_EARRING] as JewelryInstanceImpl?
    val necklace: JewelryInstanceImpl? get() = equippedItems[Slot.NECKLACE] as JewelryInstanceImpl?
    val rightRing: JewelryInstanceImpl? get() = equippedItems[Slot.RIGHT_RING] as JewelryInstanceImpl?
    val leftRing: JewelryInstanceImpl? get() = equippedItems[Slot.LEFT_RING] as JewelryInstanceImpl?
    val headgear: ArmorInstanceImpl? get() = equippedItems[Slot.HEADGEAR] as ArmorInstanceImpl?
    val oneHanded: WeaponInstanceImpl? get() = equippedItems[Slot.RIGHT_HAND] as WeaponInstanceImpl?
    val shield: ArmorInstanceImpl? get() = equippedItems[Slot.LEFT_HAND] as ArmorInstanceImpl?
    val gloves: ArmorInstanceImpl? get() = equippedItems[Slot.GLOVES] as ArmorInstanceImpl?
    val upperBody: ArmorInstanceImpl? get() = (equippedItems[Slot.UPPER_BODY]
            ?: equippedItems[Slot.UPPER_AND_LOWER_BODY]) as ArmorInstanceImpl?
    val lowerBody: ArmorInstanceImpl? get() = equippedItems[Slot.LOWER_BODY] as ArmorInstanceImpl?
    val boots: ArmorInstanceImpl? get() = equippedItems[Slot.BOOTS] as ArmorInstanceImpl?
    val underwear: ArmorInstanceImpl? get() = equippedItems[Slot.UNDERWEAR] as ArmorInstanceImpl?
    val twoHanded: WeaponInstanceImpl? get() = equippedItems[Slot.TWO_HANDS] as WeaponInstanceImpl?
    val faceAccessory: JewelryInstanceImpl? get() = equippedItems[Slot.FACE_ACCESSORY] as JewelryInstanceImpl?
    val hairAccessory: JewelryInstanceImpl? get() = equippedItems[Slot.HAIR_ACCESSORY] as JewelryInstanceImpl?

    /** Character's adena amount */
    val adena: ItemInstance? get() = findAllByTemplateId(ADENA_TEMPLATE_ID).firstOrNull()
    val weight: Int get() = items.values.sumOf { it.amount * it.weight }
    val weapon: WeaponInstanceImpl? get() = this.oneHanded ?: this.twoHanded

    operator fun get(key: Slot) = equippedItems[key]
    operator fun set(key: Slot, value: EquippableItemInstanceImpl?) {
        if (value != null) require(items.values.contains(value)) {
            "$owner tries to equip the item he does not own!!!"
        }
        equippedItems[key] = value
    }

    /** Creates new item at the inventory */
    fun createItem(templateId: Int, amount: Int = 1, equippedAt: Slot? = null, enchantLevel: Int = 0) = transaction {
        val itemTemplate = requireNotNull(ItemRegistry.findByIdOrNull(templateId)) {
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

        val instance = item.toItemInstance()

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

    /** Returns item in this inventory with [itemId], or null if no item was found */
    fun findByIdOrNull(itemId: Int) = items[itemId]

    /** Returns item with [templateId] in this inventory or null, if there is no such item */
    fun findAllByTemplateId(templateId: Int) = items.values.filter { it.templateId == templateId }

    /** Returns not equipped items by template ids */
    fun findAllNotEquippedByTemplateIds(templateIds: Iterable<Int>) = items.values.filter {
        it.equippedAt == null && templateIds.contains(it.templateId)
    }

    /** Get all the equipped items of this character */
    fun findAllEquipped() = equippedItems.values.filterNotNull()

    /** Returns item from this inventory by [itemId] or null, if it does not exist or is equipped */
    fun findNotEquippedByIdOrNull(itemId: Int) = items[itemId]?.takeIf { !it.isEquipped }

    /** Checks if item with [itemId] exists and its amount is greater or equal [amount] */
    fun existsByIdAndAmount(itemId: Int, amount: Int) = (items[itemId]?.amount ?: 0) >= amount

    /** Checks if item with [templateId] exists and its amount is greater or equal [amount] */
    fun hasEnough(templateId: Int, amount: Int) = items.values.asSequence()
        .filter { it.templateId == templateId }
        .fold(0) { acc, item -> acc + item.amount } >= amount

    /**
     * Reduces [ItemInstance.amount] on provided [value]. If [value] is equal to [ItemInstance.amount] - deletes item
     *
     * @throws IllegalArgumentException if [value] is greater than [ItemInstance.amount]
     * or there is no item by [itemId] in this inventory
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
    fun equipItem(item: EquippableItemInstanceImpl, slot: Slot): EquippableItemInstanceImpl = transaction {
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
    fun disarmItem(item: EquippableItemInstanceImpl): EquippableItemInstanceImpl = transaction {
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
        items.clear()
        items += ItemEntity.findAllByOwnerId(owner.id).map { it.toItemInstance() }.associateBy { it.id }
    }

    /** Updates the equipped items list */
    private fun updateEquippedItems() {
        equippedItems = items.values
            .filterIsInstanceAnd<EquippableItemInstanceImpl> { it.equippedAt != null }
            .associateByTo(EnumMap(Slot::class.java)) { it.equippedAt!! }
    }

    override val size by items::size
    override fun contains(element: ItemInstance) = items.values.contains(element)
    override fun containsAll(elements: Collection<ItemInstance>) = items.values.containsAll(elements)
    override fun isEmpty() = items.isEmpty()
    override fun iterator() = items.values.iterator()

    override fun toString() =
        "Inventory(ownerId=${owner.id}, items=${items.values}, equippedItems=$equippedItems, size=$size)"
}
