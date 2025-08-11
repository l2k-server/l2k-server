package org.l2kserver.game.domain

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.l2kserver.game.model.actor.character.ShortcutType

object ShortcutTable: LongIdTable("shortcuts") {
    val characterId = integer("character_id")
    val subclassIndex = integer("subclass_index")
    val index = integer("index")
    val type = postgresEnumeration<ShortcutType>("type", "SHORTCUT_TYPE")
    val shortcutActionId = integer("shortcut_action_id")
    val actionLevel = integer("action_level")

    init {
        index(isUnique = true, characterId, index, subclassIndex)
    }
}

class Shortcut(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<Shortcut>(ShortcutTable)

    val characterId by ShortcutTable.characterId
    val subclassIndex by ShortcutTable.subclassIndex
    val index by ShortcutTable.index
    val type by ShortcutTable.type
    val shortcutActionId by ShortcutTable.shortcutActionId
    val actionLevel by ShortcutTable.actionLevel

    override fun toString() = "Shortcut(characterId=$characterId, " +
            "subclassIndex=$subclassIndex, " +
            "index=$index, " +
            "type=$type, " +
            "shortcutActionId=$shortcutActionId, " +
            "actionLevel=$actionLevel)"

}
