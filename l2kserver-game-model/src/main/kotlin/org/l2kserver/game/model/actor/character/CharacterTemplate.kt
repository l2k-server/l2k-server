package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.position.Position

/**
 * Template for new character creation.
 *
 * @param race New character's race
 * @param position New character's position
 * @param items New character's starting items
 * @param collisionBox New character's hitbox
 */
data class CharacterTemplate(
    val className: CharacterClassName,
    val race: CharacterRace,
    val position: Position,
    val items: List<InitialItem>,
    val shortcuts: List<InitialShortcut>,
    val collisionBox: CollisionBox
): GameData {

    //TODO Refactor
    override val id = className.id

    object Registry: GameDataRegistry<CharacterTemplate>() {
        @JvmStatic
        fun findByClassName(characterClassName: CharacterClassName) = requireNotNull(findById(characterClassName.id)) {
            "No character class found by name $characterClassName"
        }
    }

}

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
 * @property name name of item
 * @property amount Range of item amount
 * @property isEquipped Is this item equipped (for initial items)
 * @property enchantLevel Enchant level of this item (for armor, weapons and jewellery)
 */
data class InitialItem(
    val id: Int,
    val name: String,
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
