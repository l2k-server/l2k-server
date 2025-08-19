package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.stats.Stats

data class JewelryTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,
    override val type: JewelryType,
    override val stats: Stats = Stats(),
    override val crystalCount: Int
): EquippableItemTemplate, CrystallizableItemTemplate {
    override val category = ItemCategory.JEWELRY
}

/**
 * @param availableSlots Slots, where item of this type will be placed when equipped
 *
 * @property RING Ring jewelry type
 * @property EARRING Earring jewelry type
 * @property NECKLACE Necklace jewelry type
 * @property FACE_ACCESSORY Face accessory jewelry type
 * @property HAIR_ACCESSORY Hair accessory jewelry type
 * @property TWO_SLOT_ACCESSORY Two-slot accessory jewelry type
 */
enum class JewelryType(override val availableSlots: Set<Slot>): ItemType {
    RING(setOf(Slot.LEFT_RING, Slot.RIGHT_RING)),
    EARRING(setOf(Slot.LEFT_EARRING, Slot.RIGHT_EARRING)),
    NECKLACE(setOf(Slot.NECKLACE)),
    FACE_ACCESSORY(setOf(Slot.FACE_ACCESSORY)),
    HAIR_ACCESSORY(setOf(Slot.HAIR_ACCESSORY)),
    TWO_SLOT_ACCESSORY(setOf(Slot.TWO_SLOT_ACCESSORY)),
}
