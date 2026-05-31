package org.l2kserver.game.model.item

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.stats.CombatStats

/** Stores all the item templates */
object ItemRegistry: GameDataRegistry<Item>()

/**
 * Data, common to all items of this type
 *
 * @property id Item template id
 * @property name Item name
 * @property type Item type - weapon or armor, it's group, category, slots, etc.
 * @property grade Item grade
 * @property weight Item weight
 * @property price Item price, when selling it to NPC. Don't forget about taxes!
 * @property isSellable If true, this item can be sold to NPC
 * @property isDroppable If true, this item can be dropped on the ground
 * @property isDestroyable If true, this item can be destroyed
 * @property isExchangeable If true, this item can be exchanged with other players
 */
interface Item: GameData {
    override val id: Int
    val name: String
    val type: ItemType get() = NonEquippableItemType
    val group: ItemGroup get() = ItemGroup.ETC
    val popupHintType: PopupHintType get() = PopupHintType.OTHER
    val grade: Grade get() = Grade.NO_GRADE
    val weight: Int
    val price: Int
    val isSellable: Boolean
    val isDroppable: Boolean
    val isDestroyable: Boolean
    val isExchangeable: Boolean
    val isStackable: Boolean
}

/**
 * Template of an item, that can be equipped
 *
 * @property stats Stats that will be given to the character when equipping the item
 */
interface EquippableItem: Item {
    override val type: ItemType
    override val isStackable: Boolean get() = false

    val stats: CombatStats
    val fixedBonusStats: CombatStats? get() = null
}

abstract class Book: Item {
    abstract val text: String
    final override val isStackable = false
}

abstract class Arrow: Item {
    final override val isStackable = true
    final override val type = ArrowItemType
}

/**
 * Scroll that enchant weapons, armor or jewelry
 *
 * @property target What item type does the scroll enchant
 * @property isBlessed If true, the enchanted item won't crystallize on enchant failure
 */
abstract class EnchantScroll: Item {
    abstract val target: Target
    abstract val isBlessed: Boolean

    enum class Target {
        WEAPON,
        ARMOR
    }
}

abstract class MagicItem: Item {
    final override val grade = Grade.NO_GRADE

    /**
     * Skill template that will be used when this item is used
     * Contains skill template and level
     */
    abstract val skill: ActiveSkill
}

sealed interface Shot: Item

abstract class Soulshot: Shot {
    abstract override val grade: Grade
    final override val isStackable = true
}

abstract class Spiritshot: Shot {
    abstract override val grade: Grade
    abstract val spiritshotType: Type

    final override val isStackable = true

    enum class Type {
        SPIRITSHOT,
        BLESSED_SPIRITSHOT
    }
}

abstract class Armor: EquippableItem, Crystallizable {
    abstract override val type: ArmorType

    final override val group = ItemGroup.ARMOR
    final override val popupHintType = PopupHintType.ARMOR
}

abstract class Jewelry: EquippableItem, Crystallizable {
    abstract override val type: JewelryType

    final override val group = ItemGroup.WEAPON_OR_JEWELRY
    final override val popupHintType = PopupHintType.JEWELRY
}

abstract class Weapon: EquippableItem, Crystallizable {
    abstract override val type: WeaponType

    abstract val isMagicWeapon: Boolean

    abstract val soulshotUsed: Int
    abstract val spiritshotUsed: Int

    open val consumes: ConsumableItem? get() = null
    open val manaCost: Int get() = 0

    final override val group = ItemGroup.WEAPON_OR_JEWELRY
    final override val popupHintType = PopupHintType.WEAPON
}

/** Some item group. I don't know what does it affect ¯\_(ツ)_/¯ */
enum class ItemGroup(val id: Int) {
    WEAPON_OR_JEWELRY(0),
    ARMOR(1),
    ETC(4)
}

/** Determines pop-up hint type to be displayed by client */
enum class PopupHintType {
    /** Displays weapon type and it's pAtk, mAtk, atkSpeed stats */
    WEAPON,

    /** Displays armor type and it's p.def and shield def rate (if it is Shield) stats */
    ARMOR,

    /** Displays jewelry type and it's m.def stat */
    JEWELRY,

    /** Displays 'Quest Item' on item icon hover */
    QUEST_ITEM,

    /** TODO ¯\_(ツ)_/¯ Seems it works the same as 'OTHER' */
    MONEY,

    /** Displays weight and item description (if exists)*/
    OTHER,

    /** TODO ¯\_(ツ)_/¯ */
    PET_WOLF,

    /** TODO ¯\_(ツ)_/¯ */
    PET_HATCHLING,

    /** TODO ¯\_(ツ)_/¯ */
    PET_STRIDER,

    /** TODO ¯\_(ツ)_/¯ */
    PET_BABY;

    val id = this.ordinal
}

enum class Grade {
    NO_GRADE, D, C, B, A, S
}

/** Slot, where the item should be placed (at paperdoll) */
enum class Slot(val id: Int) {
    //    INVENTORY(0),
    UNDERWEAR(1),
    RIGHT_EARRING(2),
    LEFT_EARRING(4),
    //    EARRING(6),
    NECKLACE(8),
    RIGHT_RING(16),
    LEFT_RING(32),
    //    RING(48),
    HEADGEAR(64),

    RIGHT_HAND(128),
    LEFT_HAND(256),
    GLOVES(512),
    UPPER_BODY(1024),
    LOWER_BODY(2048),
    BOOTS(4096),

    //CLOAK(8192),
    TWO_HANDS(16384),
    UPPER_AND_LOWER_BODY(32768),
    HAIR_ACCESSORY(65536),

    //TODO seems like there must be extra slot at paperdoll for pet summoning item, but I'm not sure...
    //WOLF (131072),
    //HATCHLING(1048576),
    //STRIDER(2097152),
    //BABY_PET(4194304),
    FACE_ACCESSORY(262144),
    TWO_SLOT_ACCESSORY(524288);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid slot id '$id'" }
    }
}
