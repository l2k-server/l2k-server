package org.l2kserver.game.extensions.model.shortcut

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.domain.ShortcutTable
import org.l2kserver.game.model.actor.character.ShortcutType

fun Shortcut.Companion.create(
    characterId: Int,
    subclassIndex: Int,
    shortcutIndex: Int,
    shortcutType: ShortcutType,
    shortcutActionId: Int,
    shortcutActionLevel: Int
): Shortcut {
    val shortcutId = ShortcutTable.insertAndGetId {
        it[ShortcutTable.characterId] = characterId
        it[ShortcutTable.subclassIndex] = subclassIndex
        it[index] = shortcutIndex
        it[type] = shortcutType
        it[ShortcutTable.shortcutActionId] = shortcutActionId
        it[actionLevel] = shortcutActionLevel
    }

    return findById(shortcutId)!!
}

fun Shortcut.Companion.findAllBy(characterId: Int, subclassIndex: Int) =
    find { (ShortcutTable.characterId eq characterId) and (ShortcutTable.subclassIndex eq subclassIndex) }.toList()

fun Shortcut.Companion.findBy(index: Int, characterId: Int, subclassIndex: Int) = find {
    (ShortcutTable.index eq index) and
            (ShortcutTable.characterId eq characterId) and
            (ShortcutTable.subclassIndex eq subclassIndex)
}.firstOrNull()

fun Shortcut.Companion.deleteBy(characterId: Int, subclassIndex: Int, index: Int) {
    ShortcutTable.deleteWhere {
        (this.characterId eq characterId) and (this.subclassIndex eq subclassIndex) and (this.index eq index)
    }
}
