package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.stats.CombatStats

private const val JEWELRY_SAFE_ENCHANT_LEVEL = 3
private const val JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS = 3
private const val JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS = 1

class JewelryInstanceImpl(
    entity: ItemEntity, private val template: Jewelry
): JewelryInstance, EquippableItemInstanceImpl(entity, template) {
    override val type = template.type
    override val crystalCount = template.crystalCount

    override val stats: CombatStats get() {
        val initialStats = template.stats
        if (grade == Grade.NO_GRADE) return initialStats

        val safeEnchantBonus = minOf(enchantLevel, JEWELRY_SAFE_ENCHANT_LEVEL) *
                JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS
        val unsafeEnchantBonus = maxOf(enchantLevel - JEWELRY_SAFE_ENCHANT_LEVEL, 0) *
                JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS

        return initialStats.copy(mDef = initialStats.mDef + safeEnchantBonus + unsafeEnchantBonus)
    }

    override val fixedBonusStats = template.fixedBonusStats
}
