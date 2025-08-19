package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.stats.Stats

data class ArmorTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,
    override val type: ArmorType,
    override val stats: Stats,
    override val crystalCount: Int
): EquippableItemTemplate, CrystallizableItemTemplate {
    override val category = ItemCategory.ARMOR
}

/**
 * Type of armor item
 *
 * @param availableSlots Slots, where item of this type will be placed when equipped
 */
enum class ArmorType(override val availableSlots: Set<Slot>): ItemType {
    UNDERWEAR(setOf(Slot.UNDERWEAR)),
    UPPER_BODY_LIGHT(setOf(Slot.UPPER_BODY)),
    UPPER_BODY_HEAVY(setOf(Slot.UPPER_BODY)),
    UPPER_BODY_ROBE(setOf(Slot.UPPER_BODY)),
    LOWER_BODY_LIGHT(setOf(Slot.LOWER_BODY)),
    LOWER_BODY_HEAVY(setOf(Slot.LOWER_BODY)),
    LOWER_BODY_ROBE(setOf(Slot.LOWER_BODY)),
    UPPER_AND_LOWER_BODY_LIGHT(setOf(Slot.UPPER_AND_LOWER_BODY)),
    UPPER_AND_LOWER_BODY_HEAVY(setOf(Slot.UPPER_AND_LOWER_BODY)),
    UPPER_AND_LOWER_BODY_ROBE(setOf(Slot.UPPER_AND_LOWER_BODY)),
    HEADGEAR(setOf(Slot.HEADGEAR)),
    GLOVES(setOf(Slot.GLOVES)),
    BOOTS(setOf(Slot.BOOTS)),
    SHIELD(setOf(Slot.LEFT_HAND))
}
