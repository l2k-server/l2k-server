package org.l2kserver.game.model.item

import kotlin.random.Random

/**
 * Type of item
 *
 * @property availableSlots Slots, where item of this type will be placed when equipped
 */
interface ItemType {
    val availableSlots: Set<Slot>
}

object NonEquippableItemType : ItemType {
    override val availableSlots: Set<Slot> = emptySet()
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

/**
 * @property availableSlots Slots, where item of this type will be placed when equipped
 * @property damageSpread Damage spread of weapon type.
 * If damageSpread = 0.01 and pAtk is equal 100, it means weapon can hit from 90 to 110 damage
 */
enum class WeaponType(override val availableSlots: Set<Slot>, val damageSpread: Double): ItemType {
    /** Dagger weapon type */
    DAGGER(setOf(Slot.RIGHT_HAND), 0.05),

    /** One-handed sword weapon type */
    SWORD_ONE_HANDED(setOf(Slot.RIGHT_HAND), 0.1),

    /** Two-handed sword weapon type */
    SWORD_TWO_HANDED(setOf(Slot.TWO_HANDS), 0.1),

    /** One-handed blunt weapon type */
    BLUNT_ONE_HANDED(setOf(Slot.RIGHT_HAND), 0.2),

    /** Two-handed blunt weapon type */
    BLUNT_TWO_HANDED(setOf(Slot.TWO_HANDS), 0.2),

    /** Double blades weapon type */
    DOUBLE_BLADES(setOf(Slot.TWO_HANDS), 0.1),

    /** Bow weapon type */
    BOW(setOf(Slot.TWO_HANDS), 0.05),

    /** Fist weapon type */
    FIST(setOf(Slot.TWO_HANDS), 0.05),

    /** Pole weapon type */
    POLE(setOf(Slot.TWO_HANDS), 0.1),

    /** Etc weapon type (magic books, etc.) */
    ETC(setOf(Slot.TWO_HANDS), 0.1);

    fun calculateRandomDamageModifier() = 1.0 + this.damageSpread.let { Random.nextDouble(-it, it) }

    fun isSword() = this == SWORD_ONE_HANDED || this == SWORD_TWO_HANDED
    fun isBlunt() = this == BLUNT_ONE_HANDED || this == BLUNT_TWO_HANDED
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

object ArrowItemType : ItemType {
    override val availableSlots = setOf(Slot.LEFT_HAND)
}
