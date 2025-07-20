package org.l2kserver.game.extensions

import org.jetbrains.exposed.sql.selectAll
import org.l2kserver.game.domain.shortcut.Shortcut
import org.l2kserver.game.domain.shortcut.ShortcutTable

fun Shortcut.Companion.findAllByCharacterId(characterId: Int) = ShortcutTable.selectAll()
    .where { ShortcutTable.characterId eq characterId }
    .toList()
