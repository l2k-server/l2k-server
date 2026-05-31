package org.l2kserver.game.model.item

import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
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
 * @property popUpHintType Type of popup hint to be displayed on this item icon hover
 * @property group Item group (I don't know what does it affect ¯\_(ツ)_/¯)
 */
interface ItemInstance {
    val id: Int
    val templateId: Int
    val ownerId: Int
    val amount: Int
    val equippedAt: Slot?
    val enchantLevel: Int
    val augmentationId: Int
    val name: String
    val type: ItemType
    val grade: Grade
    val weight: Int
    val price: Int
    val isSellable: Boolean
    val isDroppable: Boolean
    val isDestroyable: Boolean
    val isExchangeable: Boolean
    val isStackable: Boolean
    val popUpHintType: PopupHintType
    val group: ItemGroup

    val isEquipped: Boolean get() = equippedAt != null
}

/**
 * In-game item instance, that can be equipped
 *
 * @property grade Grade of this equippable item
 * @property stats Stats that will be given to the character when equipping the item
 * @property fixedBonusStats stats that will be applied to the character at the very end after recalculating
 * all stat bonuses and multipliers, in the form of fixed values
 * (no percentage bonuses, basic stats modifiers etc. will be applied to these stats)
 * @property equippedAt Slot, at which this item is equipped
 * @property isEquipped Is this item equipped
 */
interface EquippableItemInstance : ItemInstance {
    override val grade: Grade
    val stats: CombatStats
    val fixedBonusStats: CombatStats?
    override val equippedAt: Slot?

    override val isStackable: Boolean get() = false
}

/**
 * In-game item instance, that can be used to cast magic
 */
interface MagicItemInstance: ItemInstance {
    /** Creates skill instance for the given character ID */
    fun createSkill(characterId: Int): ActiveSkillInstance
}

/**
 * Common marker interface for both soulshot and spiritshot
 */
sealed interface ShotInstance: ItemInstance

/**
 * In-game soulshot item instance
 */
interface SoulshotInstance: ShotInstance

/**
 * In-game spiritshot item instance
 *
 * @property spiritshotType Is this blessed or regular spiritshot
 */
interface SpiritshotInstance: ShotInstance {
    val spiritshotType: Spiritshot.Type
}

/**
 * In-game armor item instance
 */
interface ArmorInstance: EquippableItemInstance, Crystallizable {
    override val type: ArmorType
}

/**
 * In-game jewelry item instance
 */
interface JewelryInstance: EquippableItemInstance, Crystallizable {
    override val type: JewelryType
}

/**
 * In-game weapon item instance
 *
 * @property isMagicWeapon Is this weapon intended for magic
 * @property soulshotUsed How many soulshots does this weapon use per hit
 * @property spiritshotUsed How manu spiritshots does this weapon use per hit
 *
 * @property soulshotCharged Is soulshot charged for next attack
 * @property spiritshotChargedType What type of spiritshot is charged for next magic casting.
 * If null - spiritshot is not charged
 *
 * @property manaCost How much mana does the auto-attack of this weapon cost
 * @property consumes How many and what items does the auto-attack of this weapon consume
 */
interface WeaponInstance: EquippableItemInstance, Crystallizable {
    override val type: WeaponType
    val isMagicWeapon: Boolean

    val soulshotUsed: Int
    val spiritshotUsed: Int

    val soulshotCharged: Boolean
    val spiritshotChargedType: Spiritshot.Type?

    val manaCost: Int
    val consumes: ConsumableItem?
}
