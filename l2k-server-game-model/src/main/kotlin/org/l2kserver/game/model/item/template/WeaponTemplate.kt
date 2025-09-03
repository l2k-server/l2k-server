package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.item.ConsumableItem
import org.l2kserver.game.model.stats.Stats
import kotlin.random.Random

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
    POLE(setOf(Slot.TWO_HANDS), 0.1);

    fun calculateRandomDamageModifier() = 1.0 + this.damageSpread.let { Random.nextDouble(-it, it) }
}

fun WeaponType?.calculateRandomDamageModifier() = this?.calculateRandomDamageModifier() ?: 1.0
