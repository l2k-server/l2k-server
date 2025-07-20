package org.l2kserver.game.domain.character

import com.fasterxml.jackson.annotation.JsonRootName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.domain.shortcut.ShortcutType
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.CollisionBox
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.actor.enumeration.CharacterClassName
import org.l2kserver.game.model.actor.enumeration.CharacterRace
import org.l2kserver.game.model.item.InitialItem
import org.l2kserver.game.utils.GameDataLoader

/**
 * Template for new character creation.
 *
 * @param race New character's race
 * @param position New character's position
 * @param items New character's starting items
 * @param collisionBox New character's hitbox
 */
@JsonRootName("characterTemplate")
data class CharacterTemplate(
    val className: CharacterClassName,
    val race: CharacterRace,
    val position: Position,
    val items: List<InitialItem>,
    val shortcuts: List<InitialShortcut>,
    val collisionBox: CollisionBox
) {

    companion object {
        private val characterTemplates = ConcurrentHashMap<CharacterClassName, CharacterTemplate>()
        private val log = logger()

        init {
            log.info("Loading character classes...")
            GameDataLoader.scanDirectory(File("./data/character/character_template"), CharacterTemplate::class.java)
                .forEach { characterTemplates[it.className] = it }
        }

        fun findByName(characterClassName: CharacterClassName) =
            requireNotNull(characterTemplates[characterClassName]) {
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
