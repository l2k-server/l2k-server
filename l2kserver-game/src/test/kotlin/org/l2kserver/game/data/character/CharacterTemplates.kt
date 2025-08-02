package org.l2kserver.game.data.character

import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterClassName
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.position.Position

val HUMAN_FIGHTER_TEMPLATE = CharacterTemplate(
    className = CharacterClassName.HUMAN_FIGHTER,
    race = CharacterRace.HUMAN,
    position = Position(-71338, 258271, -3104),
    items = listOf(
        InitialItem(1146, "Squire's Shirt", isEquipped = true),
        InitialItem(1147, "Squire's Pants", isEquipped = true),
        InitialItem(2369, "Squire's Sword", isEquipped = true),
        InitialItem(10, "Dagger", isEquipped = false),
    ),
    shortcuts = listOf(
        InitialShortcut(0, ShortcutType.ACTION, 2),
        InitialShortcut(3, ShortcutType.ACTION, 5),
        InitialShortcut(10, ShortcutType.ACTION, 0)
    ),
    collisionBox = CollisionBox(9.0, 23.0)
)
