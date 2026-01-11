package org.l2kserver.game.repository

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.domain.ShortcutTable
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.ShortcutType
import org.springframework.stereotype.Component

@Component
class ShortcutRepository {

    //TODO recreate shortcut if shortcut already exists by these shortcutIndex, characterId and subclassIndex
    fun create(
        characterId: Int,
        subclassIndex: Int,
        shortcutIndex: Int,
        shortcutType: ShortcutType,
        shortcutActionId: Int,
        shortcutActionLevel: Int
    ): Shortcut = transaction {
        val shortcutId = ShortcutTable.insertAndGetId {
            it[ShortcutTable.characterId] = characterId
            it[ShortcutTable.subclassIndex] = subclassIndex
            it[index] = shortcutIndex
            it[type] = shortcutType
            it[ShortcutTable.shortcutActionId] = shortcutActionId
            it[actionLevel] = shortcutActionLevel
        }

        Shortcut.findById(shortcutId)!!
    }

    fun createAllFrom(
        initialShortcuts: Iterable<InitialShortcut>, character: PlayerCharacterInstanceImpl
    ) = transaction {
        initialShortcuts.mapNotNull {
            val shortcutActionId = if (it.type == ShortcutType.ITEM) {
                character.inventory.findAllByTemplateId(it.shortcutActionId).firstOrNull()?.id
                    ?: return@mapNotNull null
            }
            else it.shortcutActionId
            create(
                characterId = character.id,
                subclassIndex = 0,
                shortcutIndex = it.index,
                shortcutType = it.type,
                shortcutActionId = shortcutActionId,
                shortcutActionLevel = it.actionLevel
            )
        }
    }

    fun findAllBy(characterId: Int, subclassIndex: Int) = transaction {
        Shortcut.find {
            (ShortcutTable.characterId eq characterId) and (ShortcutTable.subclassIndex eq subclassIndex)
        }.toList()
    }


    fun findBy(index: Int, characterId: Int, subclassIndex: Int) = transaction {
        Shortcut.find {
            (ShortcutTable.index eq index) and
                    (ShortcutTable.characterId eq characterId) and
                    (ShortcutTable.subclassIndex eq subclassIndex)
        }.firstOrNull()
    }

    fun deleteBy(characterId: Int, subclassIndex: Int, index: Int) = transaction {
        ShortcutTable.deleteWhere {
            (this.characterId eq characterId) and (this.subclassIndex eq subclassIndex) and (this.index eq index)
        }
    }

}
