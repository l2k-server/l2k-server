package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.item.Crystallizable
import org.l2kserver.game.model.stats.CombatStats

abstract class ArmorTemplate: EquippableItemTemplate, Crystallizable {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean
    abstract override val type: ArmorType
    abstract override val stats: CombatStats
    override val fixedBonusStats: CombatStats? get() = null
    abstract override val crystalCount: Int

    final override val popupHintType = PopupHintType.ARMOR
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
