package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.stats.CombatStats

// For full body armor safe enchant level is 4, but stats are calculated as it is 3
private const val ARMOR_SAFE_ENCHANT_LEVEL = 3
private const val ARMOR_PER_UNSAFE_ENCHANT_P_DEF_BONUS = 3
private const val ARMOR_PER_SAFE_ENCHANT_P_DEF_BONUS = 1

class ArmorInstanceImpl(
    entity: ItemEntity, private val template: Armor
): ArmorInstance, EquippableItemInstanceImpl(entity, template) {
    override val type = template.type
    override val crystalCount = template.crystalCount

    override val stats: CombatStats get() {
        if (grade == Grade.NO_GRADE) return template.stats

        val initialStats = this.template.stats
        val safeEnchantBonus = minOf(enchantLevel, ARMOR_SAFE_ENCHANT_LEVEL) *
                ARMOR_PER_SAFE_ENCHANT_P_DEF_BONUS
        val unsafeEnchantBonus = maxOf(enchantLevel - ARMOR_SAFE_ENCHANT_LEVEL, 0) *
                ARMOR_PER_UNSAFE_ENCHANT_P_DEF_BONUS

        return if (this.type == ArmorType.SHIELD)
            initialStats.copy(shieldDef = initialStats.shieldDef + safeEnchantBonus + unsafeEnchantBonus)
        else initialStats.copy(pDef = initialStats.pDef + safeEnchantBonus + unsafeEnchantBonus)
    }

    override val fixedBonusStats = template.fixedBonusStats

    override fun toString() = "Armor(name=$name id=$id enchantLevel=$enchantLevel)"
}
