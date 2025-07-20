package org.l2kserver.game.domain.item.template

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import org.l2kserver.game.model.stats.Stats


@JsonRootName("jewelry")
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
    @JsonProperty("Ring") RING(setOf(Slot.LEFT_RING, Slot.RIGHT_RING)),
    @JsonProperty("Earring") EARRING(setOf(Slot.LEFT_EARRING, Slot.RIGHT_EARRING)),
    @JsonProperty("Necklace") NECKLACE(setOf(Slot.NECKLACE)),
    @JsonProperty("Face accessory") FACE_ACCESSORY(setOf(Slot.FACE_ACCESSORY)),
    @JsonProperty("Hair accessory") HAIR_ACCESSORY(setOf(Slot.HAIR_ACCESSORY)),
    @JsonProperty("Two-slot accessory") TWO_SLOT_ACCESSORY(setOf(Slot.TWO_SLOT_ACCESSORY)),
}
