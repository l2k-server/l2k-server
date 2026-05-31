package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.stats.CombatStats

private const val WEAPON_SAFE_ENCHANT_LEVEL = 3
private const val WEAPON_UNSAFE_ENCHANT_BONUS_MULTIPLIER = 2

private const val WEAPON_D_GRADE_PER_ENCHANT_P_ATK_BONUS = 2
private const val BOW_D_GRADE_PER_ENCHANT_P_ATK_BONUS = 4
private const val WEAPON_D_GRADE_PER_ENCHANT_M_ATK_BONUS = 2

private const val ONE_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS = 3
private const val TWO_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS = 4
private const val BOW_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS = 6
private const val WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS = 3

private const val ONE_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS = 4
private const val TWO_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS = 5
private const val BOW_A_GRADE_PER_ENCHANT_P_ATK_BONUS = 8
private const val WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS = 3

private const val ONE_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS = 5
private const val TWO_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS = 6
private const val BOW_S_GRADE_PER_ENCHANT_P_ATK_BONUS = 10
private const val WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS = 4

class WeaponInstanceImpl(
    entity: ItemEntity,
    private val template: Weapon
): WeaponInstance, EquippableItemInstanceImpl(entity, template) {
    override val name = template.name
    override val grade = template.grade
    override val weight = template.weight
    override val price = template.price
    override val isSellable = template.isSellable
    override val isDroppable = template.isDroppable
    override val isDestroyable = template.isDestroyable
    override val isExchangeable = template.isExchangeable
    override val type = template.type
    override val isMagicWeapon = template.isMagicWeapon
    override val crystalCount = template.crystalCount

    override val soulshotUsed = template.soulshotUsed
    override val spiritshotUsed = template.spiritshotUsed

    override var soulshotCharged = false
    override var spiritshotChargedType: Spiritshot.Type? = null

    override val manaCost = template.manaCost
    override val consumes = template.consumes

    override val stats: CombatStats get() {
        val initialStats = template.stats

        val (pAtkPerEnchantBonus, mAtkPerEnchantBonus) = when (grade) {
            Grade.NO_GRADE -> 0 to 0
            Grade.D -> when (type) {
                WeaponType.BOW -> BOW_D_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_D_GRADE_PER_ENCHANT_M_ATK_BONUS
                else -> WEAPON_D_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_D_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.C, Grade.B -> when (type) {
                WeaponType.DAGGER,
                WeaponType.SWORD_ONE_HANDED,
                WeaponType.BLUNT_ONE_HANDED,
                WeaponType.POLE,
                WeaponType.ETC ->
                    ONE_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED,
                WeaponType.BLUNT_TWO_HANDED,
                WeaponType.DOUBLE_BLADES,
                WeaponType.FIST ->
                    TWO_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.A -> when (type) {
                WeaponType.DAGGER,
                WeaponType.SWORD_ONE_HANDED,
                WeaponType.BLUNT_ONE_HANDED,
                WeaponType.POLE,
                WeaponType.ETC ->
                    ONE_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED,
                WeaponType.BLUNT_TWO_HANDED,
                WeaponType.DOUBLE_BLADES,
                WeaponType.FIST ->
                    TWO_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.S -> when (type) {
                WeaponType.DAGGER,
                WeaponType.SWORD_ONE_HANDED,
                WeaponType.BLUNT_ONE_HANDED,
                WeaponType.POLE,
                WeaponType.ETC ->
                    ONE_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED,
                WeaponType.BLUNT_TWO_HANDED,
                WeaponType.DOUBLE_BLADES,
                WeaponType.FIST ->
                    TWO_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
        }

        val safeEnchantLevel = minOf(enchantLevel, WEAPON_SAFE_ENCHANT_LEVEL)
        val unsafeEnchantLevel = maxOf(enchantLevel - WEAPON_SAFE_ENCHANT_LEVEL, 0)

        return initialStats.copy(
            pAtk = initialStats.pAtk + safeEnchantLevel * pAtkPerEnchantBonus +
                    unsafeEnchantLevel * pAtkPerEnchantBonus * WEAPON_UNSAFE_ENCHANT_BONUS_MULTIPLIER,
            mAtk = initialStats.mAtk + safeEnchantLevel * mAtkPerEnchantBonus +
                    unsafeEnchantLevel * mAtkPerEnchantBonus * WEAPON_UNSAFE_ENCHANT_BONUS_MULTIPLIER
        )
    }

    override val fixedBonusStats = template.fixedBonusStats

    override fun toString() = "Weapon(name=$name id=$id enchantLevel=$enchantLevel)"
}
