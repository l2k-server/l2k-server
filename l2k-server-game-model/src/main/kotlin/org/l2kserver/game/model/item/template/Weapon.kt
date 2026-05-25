package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.item.Crystallizable
import org.l2kserver.game.model.stats.CombatStats
import kotlin.random.Random

abstract class Weapon: EquippableItem, Crystallizable {
    abstract override val id: Int
    abstract override val name: String
    abstract override val grade: Grade
    abstract override val weight: Int
    abstract override val price: Int
    abstract override val isSellable: Boolean
    abstract override val isDroppable: Boolean
    abstract override val isDestroyable: Boolean
    abstract override val isExchangeable: Boolean
    abstract override val type: WeaponType
    abstract override val stats: CombatStats

    abstract override val crystalCount: Int

    abstract val soulshotUsed: Int
    abstract val spiritshotUsed: Int

    override val fixedBonusStats: CombatStats? get() = null

    open val consumes: ConsumableItem? get() = null
    open val manaCost: Int get() = 0

    final override val popupHintType = PopupHintType.WEAPON
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

fun WeaponType?.calculateRandomDamageModifier() = this?.calculateRandomDamageModifier() ?: 1.0
