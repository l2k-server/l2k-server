package org.l2kserver.game.data.character.classes

import org.l2kserver.game.data.item.armor.SQUIRES_PANTS
import org.l2kserver.game.data.item.armor.SQUIRES_SHIRT
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.SQUIRES_SWORD
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.L2kCharacterClass
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.PerLevelGain
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CON
import org.l2kserver.game.model.stats.DEX
import org.l2kserver.game.model.stats.INT
import org.l2kserver.game.model.stats.MEN
import org.l2kserver.game.model.stats.STR
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.stats.WIT

val HUMAN_FIGHTER = L2kCharacterClass(
    id = 0,
    requiredLevel = 1,
    combatStats = Stats(
        maxCp = 32,
        maxHp = 80,
        maxMp = 30,

        speed = 115,
        castingSpd = 333,

        hpRegen = 1.5,
        mpRegen = 0.9,
        cpRegen = 1.5,
    ),
    basicStats = BasicStats(
        STR(40),
        DEX(30),
        CON(43),
        INT(21),
        WIT(11),
        MEN(25),
    ),
    tradeAndInventoryStats = TradeAndInventoryStats(
        privateStoreSize = 4
    ),
    emptySlotStats = FIGHTER_EMPTY_SLOT_STATS,
    perLevelGain = PerLevelGain(
        cpAdd = 4.73,
        cpMod = 0.22,
        hpAdd = 11.83,
        hpMod = 0.37,
        mpAdd = 5.46,
        mpMod = 0.14,
    ),
    characterTemplate = CharacterTemplate(
        position = Position(-71338, 258271, -3104),
        items = listOf(
            InitialItem(SQUIRES_SHIRT.id, isEquipped = true),
            InitialItem(SQUIRES_PANTS.id, isEquipped = true),
            InitialItem(SQUIRES_SWORD.id, isEquipped = true),
            InitialItem(DAGGER.id, isEquipped = false),
        ),
        shortcuts = listOf(
            InitialShortcut(0, ShortcutType.ACTION, 2),
            InitialShortcut(3, ShortcutType.ACTION, 5),
            InitialShortcut(10, ShortcutType.ACTION, 0)
        ),
        collisionBox = CollisionBox(9.0, 23.0)
    )
)
