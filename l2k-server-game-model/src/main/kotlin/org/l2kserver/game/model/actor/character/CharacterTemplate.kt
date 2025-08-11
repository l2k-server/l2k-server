package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.position.Position

/**
 * Template for new character creation.
 *
 * @property position New character's position
 * @property items New character's starting items
 * @property shortcuts New character's starting shortcuts
 * @property collisionBox New character's collision box
 */
data class CharacterTemplate(
    val position: Position,
    val items: List<InitialItem>,
    val shortcuts: List<InitialShortcut>,
    val collisionBox: CollisionBox
)

data class InitialShortcut(
    val index: Int,
    val type: ShortcutType,
    val shortcutActionId: Int,
    val actionLevel: Int = 1
)

//TODO попробовать тоже через полиморфизм
enum class ShortcutType(val id: Int) {
    ITEM(1),
    SKILL(2),
    ACTION(3),
    MACRO(4),
    RECIPE(5);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid shortcut type id '$id'" }
    }

}

/**
 * An item that must be given to character on creation
 *
 * @property id id of item kind (for example Squire's Shirt's item id is 1146)
 * @property amount Range of item amount
 * @property isEquipped Is this item equipped (for initial items)
 * @property enchantLevel Enchant level of this item (for armor, weapons and jewellery)
 */
data class InitialItem(
    val id: Int,
    val amount: Int = 1,
    val isEquipped: Boolean = false,
    val enchantLevel: Int = 0
)

enum class CharacterRace {
    HUMAN,
    ELF,
    DARK_ELF,
    ORC,
    DWARF
}
