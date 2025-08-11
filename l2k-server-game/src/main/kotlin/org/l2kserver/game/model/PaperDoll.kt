package org.l2kserver.game.model

import java.util.EnumMap
import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.extensions.filterIsInstanceAnd
import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.EquippableItem
import org.l2kserver.game.model.item.Item
import org.l2kserver.game.model.item.Jewelry
import org.l2kserver.game.model.item.Slot
import org.l2kserver.game.model.item.Weapon

/**
 * All the equipped items
 *
 * @param equippedItems Equipped player items.
 */
@JvmInline
value class PaperDoll(val equippedItems: MutableMap<Slot, EquippableItem?>) {

    /**
     * @param items Player items. Equipped items will be placed to paper doll, other will be ignored
     */
    constructor(items: Iterable<Item> = emptyList()) : this(
        items.filterIsInstanceAnd<EquippableItem> { it.equippedAt != null }
            .associateByTo(EnumMap(Slot::class.java)) { it.equippedAt!! }
    )

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

    operator fun get(key: Slot) = equippedItems[key]
    operator fun set(key: Slot, value: EquippableItem?) { equippedItems[key] = value }

    fun getWeapon() = this.oneHanded ?: this.twoHanded

    /**
     * Equips this item - stores it to [slot] at paper doll and updates database
     *
     * @return equipped item
     */
    fun equipItem(item: EquippableItem, slot: Slot): EquippableItem {
        require(item.type.availableSlots.contains(slot)) { "$item cannot be equipped to $slot" }
        item.equippedAt = slot
        this[slot] = item

        return item
    }

    /**
     * Disarms this item - replaces it from slot at paper doll and updates database
     *
     * @return disarmed item
     */
    fun disarmItem(item: EquippableItem): EquippableItem = transaction {
        item.equippedAt?.let {
            this@PaperDoll[it] = null
            item.equippedAt = null
        }
        return@transaction item
    }

}
