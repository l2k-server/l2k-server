package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.CrystallizableItemInstance
import org.l2kserver.game.model.item.instance.EquippableItemInstance
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.JewelryTemplate
import org.l2kserver.game.model.stats.Stats

private const val JEWELRY_SAFE_ENCHANT_LEVEL = 3

private const val JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS = 3
private const val JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS = 1

class Jewelry(
    private val itemEntity: ItemEntity,
    private val itemTemplate: JewelryTemplate,
): EquippableItemInstance, CrystallizableItemInstance {
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

    override val category = itemTemplate.category
    override val group = ItemGroup.WEAPON_OR_JEWELRY

    override fun toString() = "Jewelry(name=$name id=$id enchantLevel=$enchantLevel)"

    override val stats: Stats get() {
        if (grade == Grade.NO_GRADE) return itemTemplate.stats

        val initialStats = itemTemplate.stats
        val safeEnchantBonus = minOf(enchantLevel, JEWELRY_SAFE_ENCHANT_LEVEL) * JEWELRY_PER_SAFE_ENCHANT_P_DEF_BONUS
        val unsafeEnchantBonus = maxOf(enchantLevel - JEWELRY_SAFE_ENCHANT_LEVEL, 0) * JEWELRY_PER_UNSAFE_ENCHANT_P_DEF_BONUS

        return initialStats.copy(mDef = initialStats.mDef + safeEnchantBonus + unsafeEnchantBonus)
    }

}
