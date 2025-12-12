package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.JewelryInstance
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.JewelryTemplate
import org.l2kserver.game.model.stats.CombatStats

private const val JEWELRY_SAFE_ENCHANT_LEVEL = 3
private const val JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS = 3
private const val JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS = 1

class Jewelry(itemEntity: ItemEntity, private val itemTemplate: JewelryTemplate): JewelryInstance {
    override val id: Int = itemEntity.id.value
    override val templateId = itemEntity.templateId

    override var ownerId by itemEntity::ownerId
    override var amount by itemEntity::amount
    override var equippedAt by itemEntity::equippedAt
    override var enchantLevel by itemEntity::enchantLevel

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

    override val stats: CombatStats get() {
        val initialStats = itemTemplate.stats
        if (grade == Grade.NO_GRADE) return initialStats

        val safeEnchantBonus = minOf(enchantLevel, JEWELRY_SAFE_ENCHANT_LEVEL) *
                JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS
        val unsafeEnchantBonus = maxOf(enchantLevel - JEWELRY_SAFE_ENCHANT_LEVEL, 0) *
                JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS

        return initialStats.copy(mDef = initialStats.mDef + safeEnchantBonus + unsafeEnchantBonus)
    }

    override val fixedBonusStats = itemTemplate.fixedBonusStats
}
