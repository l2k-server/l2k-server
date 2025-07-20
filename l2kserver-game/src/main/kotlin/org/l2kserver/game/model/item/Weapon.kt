package org.l2kserver.game.model.item

import org.l2kserver.game.domain.item.entity.ItemEntity
import org.l2kserver.game.domain.item.template.Grade
import org.l2kserver.game.domain.item.template.ItemGroup
import org.l2kserver.game.domain.item.template.WeaponTemplate
import org.l2kserver.game.domain.item.template.WeaponType
import org.l2kserver.game.model.stats.Stats

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

class Weapon(
    itemEntity: ItemEntity,
    private val itemTemplate: WeaponTemplate
) : EquippableItem, CrystallizableItem {
    override val id: Int = itemEntity.id.value

    override val templateId by itemEntity::templateId
    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount
    override var equippedAt by itemEntity::equippedAt
    override var enchantLevel by itemEntity::enchantLevel
    override var augmentationId by itemEntity::augmentationId

    override val name = itemTemplate.name
    override val grade = itemTemplate.grade
    override val weight = itemTemplate.weight
    override val price = itemTemplate.price
    override val isSellable = itemTemplate.isSellable
    override val isDroppable = itemTemplate.isDroppable
    override val isDestroyable = itemTemplate.isDestroyable
    override val isExchangeable = itemTemplate.isExchangeable
    override val type = itemTemplate.type
    override val crystalCount = itemTemplate.crystalCount

    val soulshotUsed = itemTemplate.soulshotUsed
    val spiritshotUsed = itemTemplate.spiritshotUsed
    val manaCost = itemTemplate.manaCost
    val consumes = itemTemplate.consumes

    override val category = itemTemplate.category
    override val group = ItemGroup.WEAPON_OR_JEWELRY

    override fun toString() = "Weapon(name=$name id=$id enchantLevel=$enchantLevel)"

    override val stats: Stats get() {
        val (pAtkPerEnchantBonus, mAtkPerEnchantBonus) = when (grade) {
            Grade.NO_GRADE -> 0 to 0
            Grade.D -> when (type) {
                WeaponType.BOW -> BOW_D_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_D_GRADE_PER_ENCHANT_M_ATK_BONUS
                else -> WEAPON_D_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_D_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.C, Grade.B -> when (type) {
                WeaponType.DAGGER, WeaponType.SWORD_ONE_HANDED, WeaponType.BLUNT_ONE_HANDED, WeaponType.POLE ->
                    ONE_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED, WeaponType.BLUNT_TWO_HANDED, WeaponType.DOUBLE_BLADES, WeaponType.FIST ->
                    TWO_HANDED_WEAPON_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_C_B_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_C_B_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.A -> when (type) {
                WeaponType.DAGGER, WeaponType.SWORD_ONE_HANDED, WeaponType.BLUNT_ONE_HANDED, WeaponType.POLE ->
                    ONE_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED, WeaponType.BLUNT_TWO_HANDED, WeaponType.DOUBLE_BLADES, WeaponType.FIST ->
                    TWO_HANDED_WEAPON_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_A_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_A_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
            Grade.S -> when (type) {
                WeaponType.DAGGER, WeaponType.SWORD_ONE_HANDED, WeaponType.BLUNT_ONE_HANDED, WeaponType.POLE ->
                    ONE_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.SWORD_TWO_HANDED, WeaponType.BLUNT_TWO_HANDED, WeaponType.DOUBLE_BLADES, WeaponType.FIST ->
                    TWO_HANDED_WEAPON_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
                WeaponType.BOW ->
                    BOW_S_GRADE_PER_ENCHANT_P_ATK_BONUS to WEAPON_S_GRADE_PER_ENCHANT_M_ATK_BONUS
            }
        }

        val initialStats = itemTemplate.stats
        val safeEnchantLevel = minOf(enchantLevel, WEAPON_SAFE_ENCHANT_LEVEL)
        val unsafeEnchantLevel = maxOf(enchantLevel - WEAPON_SAFE_ENCHANT_LEVEL, 0)

        return initialStats.copy(
            pAtk = initialStats.pAtk + safeEnchantLevel * pAtkPerEnchantBonus + 
                    unsafeEnchantLevel * pAtkPerEnchantBonus * WEAPON_UNSAFE_ENCHANT_BONUS_MULTIPLIER,
            mAtk = initialStats.mAtk + safeEnchantLevel * mAtkPerEnchantBonus + 
                    unsafeEnchantLevel * mAtkPerEnchantBonus * WEAPON_UNSAFE_ENCHANT_BONUS_MULTIPLIER
        )
    }

}
