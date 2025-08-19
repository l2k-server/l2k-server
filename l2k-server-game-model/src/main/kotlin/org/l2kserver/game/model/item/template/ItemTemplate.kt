package org.l2kserver.game.model.item.template

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.stats.Stats

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
sealed interface ItemTemplate: GameData {
    override val id: Int
    val name: String
    val type: ItemType
    val category: ItemCategory
    val grade: Grade
    val weight: Int
    val price: Int
    val isSellable: Boolean
    val isDroppable: Boolean
    val isDestroyable: Boolean
    val isExchangeable: Boolean
    val isStackable: Boolean

    object Registry: GameDataRegistry<ItemTemplate>()
}

/**
 * Template of an item, that can be equipped
 *
 * @property stats Stats that will be given to the character when equipping the item
 */
sealed interface EquippableItemTemplate: ItemTemplate {
    val stats: Stats
    override val isStackable: Boolean get() = false
}

/**
 * Template of an item, that can be crystallized
 *
 * @property crystalCount How many crystals will be given for this item crystallization
 */
sealed interface CrystallizableItemTemplate: ItemTemplate {
    val crystalCount: Int
}

/**
 * Type of item
 *
 * @property availableSlots Slots, where item of this type will be placed when equipped
 */
interface ItemType {
    val availableSlots: Set<Slot>
}

enum class ItemGroup(val id: Int) {
    WEAPON_OR_JEWELRY(0),
    ARMOR(1),
    ETC(4)
}

enum class ItemCategory {
    WEAPON,
    ARMOR,
    JEWELRY,
    QUEST_ITEM,
    MONEY,
    OTHER,
    PET_WOLF,
    PET_HATCHLING,
    PET_STRIDER,
    PET_BABY;

    val id = this.ordinal
}

enum class Grade {
    NO_GRADE, D, C, B, A, S
}

/**
 * Slot, where the item should be placed (at paperdoll)
 **/
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
