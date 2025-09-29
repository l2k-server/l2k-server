package org.l2kserver.game.data.character.classes

import org.l2kserver.game.data.item.armor.APPRENTICE_STOCKINGS
import org.l2kserver.game.data.item.armor.APPRENTICE_TUNIC
import org.l2kserver.game.data.item.book.TUTORIAL_GUIDE
import org.l2kserver.game.data.item.weapons.APPRENTICE_WAND
import org.l2kserver.game.data.skill.WIND_STRIKE
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.PerLevelGain
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.character.SkillToLearn
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CON
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.DEX
import org.l2kserver.game.model.stats.INT
import org.l2kserver.game.model.stats.MEN
import org.l2kserver.game.model.stats.STR
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.stats.WIT

val HUMAN_MYSTIC = CharacterClass(
    id = 10,
    requiredLevel = 1,
    combatStats = CombatStats(
        maxCp = 50,
        maxHp = 101,
        maxMp = 40,

        speed = 120,
        castingSpd = 333,
    ),
    basicStats = BasicStats(
        STR(22),
        DEX(21),
        CON(27),
        INT(41),
        WIT(20),
        MEN(39),
    ),
    tradeAndInventoryStats = TradeAndInventoryStats(
        privateStoreSize = 4
    ),
    emptySlotStats = CharacterClass.DefaultEmptySlotStats.MYSTIC,
    perLevelGain = PerLevelGain(
        cpAdd = 7.84,
        cpMod = 0.22,
        hpAdd = 15.57,
        hpMod = 0.37,
        mpAdd = 7.38,
        mpMod = 0.14,
    ),
    characterTemplate = CharacterTemplate(
        position = Position(-90890, 248027, -3570),
        items = listOf(
            InitialItem(APPRENTICE_TUNIC.id, isEquipped = true),
            InitialItem(APPRENTICE_STOCKINGS.id, isEquipped = true),
            InitialItem(APPRENTICE_WAND.id, isEquipped = true),
            InitialItem(TUTORIAL_GUIDE.id, isEquipped = false)
        ),
        shortcuts = listOf(
            InitialShortcut(0, ShortcutType.ACTION, 2),
            InitialShortcut(1, ShortcutType.SKILL, WIND_STRIKE.id),
            InitialShortcut(3, ShortcutType.ACTION, 5),
            InitialShortcut(10, ShortcutType.ACTION, 0),
            InitialShortcut(11, type = ShortcutType.ITEM, TUTORIAL_GUIDE.id)
        ),
        collisionBox = CollisionBox(7.5, 22.8)
    ),
    skillTree = mapOf(
        1 to listOf(SkillToLearn(WIND_STRIKE.id, 1, autoLearn = true))
    )
)
