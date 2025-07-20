package org.l2kserver.game.domain.item.template

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import org.l2kserver.game.model.stats.Stats

@JsonRootName("weapon")
data class WeaponTemplate(
    override val id: Int,
    override val name: String,
    override val grade: Grade,
    override val weight: Int,
    override val price: Int,
    override val isSellable: Boolean,
    override val isDroppable: Boolean,
    override val isDestroyable: Boolean,
    override val isExchangeable: Boolean,
    override val type: WeaponType,
    override val stats: Stats,
    override val crystalCount: Int,

    val soulshotUsed: Int,
    val spiritshotUsed: Int,
    val consumes: ConsumableItem? = null,
    val manaCost: Int = 0
): EquippableItemTemplate, CrystallizableItemTemplate {

    override val category = ItemCategory.WEAPON
}

/**
 * @param availableSlots Slots, where item of this type will be placed when equipped
 * @param randomCoefficient How many percent will be added (or subtracted) to resulting damage
 *
 * @property DAGGER Dagger weapon type
 * @property SWORD_ONE_HANDED One-handed sword weapon type
 * @property SWORD_TWO_HANDED Two-handed sword weapon type
 * @property BLUNT_ONE_HANDED One-handed blunt weapon type
 * @property BLUNT_TWO_HANDED Two-handed blunt weapon type
 * @property DOUBLE_BLADES Double blades weapon type
 * @property BOW Bow weapon type
 * @property FIST Fist weapon type
 * @property POLE Pole weapon type
 */
enum class WeaponType(override val availableSlots: Set<Slot>, val randomCoefficient: Int): ItemType {
    @JsonProperty("Dagger")
    DAGGER(setOf(Slot.RIGHT_HAND), 5),

    @JsonProperty("Sword/One handed")
    SWORD_ONE_HANDED(setOf(Slot.RIGHT_HAND), 10),

    @JsonProperty("Sword/Two handed")
    SWORD_TWO_HANDED(setOf(Slot.TWO_HANDS), 10),

    @JsonProperty("Blunt/One handed")
    BLUNT_ONE_HANDED(setOf(Slot.RIGHT_HAND), 20),

    @JsonProperty("Blunt/Two handed")
    BLUNT_TWO_HANDED(setOf(Slot.TWO_HANDS), 20),

    @JsonProperty("Double Blades")
    DOUBLE_BLADES(setOf(Slot.TWO_HANDS), 10),

    @JsonProperty("Bow")
    BOW(setOf(Slot.TWO_HANDS), 5),

    @JsonProperty("Fist")
    FIST(setOf(Slot.TWO_HANDS), 5),

    @JsonProperty("Pole")
    POLE(setOf(Slot.TWO_HANDS), 10)
}

data class ConsumableItem(
    val id: Int,
    val amount: Int = 1
)
