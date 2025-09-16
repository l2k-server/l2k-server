package org.l2kserver.game.model.item.instance

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.ItemCategory
import org.l2kserver.game.model.item.template.ItemGroup
import org.l2kserver.game.model.item.template.ItemType
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.CombatStats

/**
 * In-game item instance
 *
 * @property id Item id
 * @property templateId Item template id (for example Squire's Shirt's itemTemplateId is 1146)
 * @property ownerId Identifier of a character, that owns this item
 * @property amount Items amount (in stack)
 * @property name Item name
 * @property type Item type - weapon or armor part, slots to equip, etc.
 * @property weight Item weight
 * @property price Item price, when selling it to NPC. Don't forget about taxes!
 * @property isSellable If true, this item can be sold to NPC
 * @property isDroppable If true, this item can be dropped on the ground
 * @property isDestroyable If true, this item can be destroyed
 * @property isExchangeable If true, this item can be exchanged with other players
 * @property category Item category
 * @property group Item group
 */
interface ItemInstance {
     val id: Int
     val templateId: Int
     var ownerId: Int
     var amount: Int
     val equippedAt: Slot? get() = null
     val enchantLevel: Int get() = 0
     val augmentationId: Int get() = 0
     val name: String
     val type: ItemType
     val grade: Grade get() = Grade.NO_GRADE
     val weight: Int
     val price: Int
     val isSellable: Boolean
     val isDroppable: Boolean
     val isDestroyable: Boolean
     val isExchangeable: Boolean
     val isStackable: Boolean
     val category: ItemCategory
     val group: ItemGroup

     val isEquipped: Boolean get() = equippedAt != null
}

/** In-game item instance, that can be used */
interface UsableItemInstance: ItemInstance

/**
 * In-game item instance, that can be equipped
 *
 * @property grade Grade of this equippable item
 * @property stats Stats that will be given to the character when equipping the item
 * @property equippedAt Slot, at which this item is equipped
 * @property isEquipped Is this item equipped
 */
interface EquippableItemInstance : ItemInstance {
     override val grade: Grade
     val stats: CombatStats
     override var equippedAt: Slot?

     override val isStackable: Boolean get() = false
}

/**
 * In-game item that can be crystallized
 *
 * @property crystalCount How many crystals will be given for this item crystallization
 */
interface CrystallizableItemInstance: ItemInstance {
    val crystalCount: Int
}

/**
 * In-game item instance, that can be enchanted
 *
 * @property grade Grande of enchant scrolls, to enchant this item
 * @property enchantLevel THis item enchant level
 */
interface EnchantableItemInstance: CrystallizableItemInstance {
     override val grade: Grade
     override var enchantLevel: Int
}

/**
 * In-game item instance, that can be augmented
 *
 * @property augmentationId TODO Augmentation skill identifier?
 */
interface AugmentableItemInstance: ItemInstance {
     override var augmentationId: Int
}
