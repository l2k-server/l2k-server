package org.l2kserver.game.data.character.classes

import org.l2kserver.game.data.item.armor.SquiresPants
import org.l2kserver.game.data.item.armor.SquiresShirt
import org.l2kserver.game.data.item.book.TutorialGuide
import org.l2kserver.game.data.item.weapons.DAGGER
import org.l2kserver.game.data.item.weapons.SquiresSword
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.CharacterClass
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
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.stats.WIT

val HUMAN_FIGHTER = CharacterClass(
    id = 0,
    requiredLevel = 1,
    combatStats = CombatStats(
        maxCp = 32,
        maxHp = 80,
        maxMp = 30,

        speed = 115,
        castingSpd = 333,
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
    emptySlotStats = CharacterClass.DefaultEmptySlotStats.FIGHTER,
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
            InitialItem(SquiresShirt.id, isEquipped = true),
            InitialItem(SquiresPants.id, isEquipped = true),
            InitialItem(SquiresSword.id, isEquipped = true),
            InitialItem(DAGGER.id, isEquipped = false),
            InitialItem(TutorialGuide.id, isEquipped = false)
        ),
        shortcuts = listOf(
            InitialShortcut(0, ShortcutType.ACTION, 2),
            InitialShortcut(3, ShortcutType.ACTION, 5),
            InitialShortcut(10, ShortcutType.ACTION, 0),
            InitialShortcut(11, type = ShortcutType.ITEM, TutorialGuide.id)
        ),
        collisionBox = CollisionBox(9.0, 23.0)
    ),
    skillTree = emptyMap()
)
