package org.l2kserver.game.domain.item.template

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import org.l2kserver.game.model.stats.Stats

@JsonRootName("armor")
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
 * @param availableSlots Slots, where item of this type will be placed when equipped
 *
 * @property UNDERWEAR Underwear armor type
 * @property UPPER_BODY_LIGHT Upper body/Light armor type
 * @property UPPER_BODY_HEAVY Upper body/Heavy armor type
 * @property UPPER_BODY_ROBE Upped body/Robe armor type
 * @property LOWER_BODY_LIGHT Lower body/Light armor type
 * @property LOWER_BODY_HEAVY Lower body/Heavy armor type
 * @property LOWER_BODY_ROBE Lower body/Robe armor type
 * @property UPPER_AND_LOWER_BODY_LIGHT Upper and Lower body/Light armor type
 * @property UPPER_AND_LOWER_BODY_HEAVY Upper and Lower body/Heavy armor type
 * @property UPPER_AND_LOWER_BODY_ROBE Upper and Lower body/Robe armor type
 * @property HEADGEAR Headgear armor type
 * @property GLOVES Gloves armor type
 * @property BOOTS Boots armor type
 * @property SHIELD Shield armor type
 */
enum class ArmorType(override val availableSlots: Set<Slot>): ItemType {
    @JsonProperty("Underwear") UNDERWEAR(setOf(Slot.UNDERWEAR)),
    @JsonProperty("Upper body/Light") UPPER_BODY_LIGHT(setOf(Slot.UPPER_BODY)),
    @JsonProperty("Upper body/Heavy") UPPER_BODY_HEAVY(setOf(Slot.UPPER_BODY)),
    @JsonProperty("Upper body/Robe") UPPER_BODY_ROBE(setOf(Slot.UPPER_BODY)),
    @JsonProperty("Lower body/Light") LOWER_BODY_LIGHT(setOf(Slot.LOWER_BODY)),
    @JsonProperty("Lower body/Heavy") LOWER_BODY_HEAVY(setOf(Slot.LOWER_BODY)),
    @JsonProperty("Lower body/Robe") LOWER_BODY_ROBE(setOf(Slot.LOWER_BODY)),
    @JsonProperty("Upper and Lower body/Light") UPPER_AND_LOWER_BODY_LIGHT(setOf(Slot.UPPER_AND_LOWER_BODY)),
    @JsonProperty("Upper and Lower body/Heavy") UPPER_AND_LOWER_BODY_HEAVY(setOf(Slot.UPPER_AND_LOWER_BODY)),
    @JsonProperty("Upper and Lower body/Robe") UPPER_AND_LOWER_BODY_ROBE(setOf(Slot.UPPER_AND_LOWER_BODY)),
    @JsonProperty("Headgear") HEADGEAR(setOf(Slot.HEADGEAR)),
    @JsonProperty("Gloves") GLOVES(setOf(Slot.GLOVES)),
    @JsonProperty("Boots") BOOTS(setOf(Slot.BOOTS)),
    @JsonProperty("Shield") SHIELD(setOf(Slot.LEFT_HAND))
}
