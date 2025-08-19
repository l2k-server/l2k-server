package org.l2kserver.game.model.item

import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.model.item.instance.CrystallizableItemInstance
import org.l2kserver.game.model.item.instance.EquippableItemInstance
import org.l2kserver.game.model.item.template.ArmorTemplate
import org.l2kserver.game.model.item.template.ArmorType
import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.stats.Stats

// For full body armor safe enchant level is 4, but stats are calculated as it is 3
private const val ARMOR_SAFE_ENCHANT_LEVEL = 3

private const val ARMOR_PER_UNSAFE_ENCHANT_P_DEF_BONUS = 3
private const val ARMOR_PER_SAFE_ENCHANT_P_DEF_BONUS = 1

class Armor(
    private val itemEntity: ItemEntity,
    private val itemTemplate: ArmorTemplate,
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
    override val group = ItemGroup.ARMOR

    override fun toString() = "Armor(name=$name id=$id enchantLevel=$enchantLevel)"

    override val stats: Stats get() {
        if (grade == Grade.NO_GRADE) return itemTemplate.stats

        val initialStats = this.itemTemplate.stats
        val safeEnchantBonus = minOf(enchantLevel, ARMOR_SAFE_ENCHANT_LEVEL) * ARMOR_PER_SAFE_ENCHANT_P_DEF_BONUS
        val unsafeEnchantBonus = maxOf(enchantLevel - ARMOR_SAFE_ENCHANT_LEVEL, 0) * ARMOR_PER_UNSAFE_ENCHANT_P_DEF_BONUS

        return if (this.type == ArmorType.SHIELD)
            initialStats.copy(shieldDef = initialStats.shieldDef + safeEnchantBonus + unsafeEnchantBonus)
        else initialStats.copy(pDef = initialStats.pDef + safeEnchantBonus + unsafeEnchantBonus)
    }

}
