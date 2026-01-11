package org.l2kserver.game.data.character.classes

import org.l2kserver.game.data.item.armor.ApprenticesStockings
import org.l2kserver.game.data.item.armor.ApprenticeTunic
import org.l2kserver.game.data.item.book.TutorialGuide
import org.l2kserver.game.data.item.weapons.ApprenticesWand
import org.l2kserver.game.data.skill.SelfHeal
import org.l2kserver.game.data.skill.WindStrike
import org.l2kserver.game.data.skill.Spellcraft
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.DefaultEmptySlotStats
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

data object HumanMystic: CharacterClass() {
    override val id = 10
    override val requiredLevel = 1
    override val combatStats = CombatStats(
        maxCp = 50,
        maxHp = 101,
        maxMp = 40,

        speed = 120,
        castingSpd = 333,
    )
    override val basicStats = BasicStats(
        STR(22),
        DEX(21),
        CON(27),
        INT(41),
        WIT(20),
        MEN(39),
    )
    override val tradeAndInventoryStats = TradeAndInventoryStats(privateStoreSize = 4)
    override val emptySlotStats = DefaultEmptySlotStats.MYSTIC

    override val parentClass = null

    override val perLevelGain = PerLevelGain(
        cpAdd = 7.84,
        cpMod = 0.22,
        hpAdd = 15.57,
        hpMod = 0.37,
        mpAdd = 7.38,
        mpMod = 0.14,
    )

    override val characterTemplate = CharacterTemplate(
        position = Position(-90890, 248027, -3570),
        items = listOf(
            InitialItem(ApprenticeTunic.id, isEquipped = true),
            InitialItem(ApprenticesStockings.id, isEquipped = true),
            InitialItem(ApprenticesWand.id, isEquipped = true),
            InitialItem(TutorialGuide.id, isEquipped = false)
        ),
        shortcuts = listOf(
            InitialShortcut(0, ShortcutType.ACTION, 2),
            InitialShortcut(1, ShortcutType.SKILL, WindStrike.id),
            InitialShortcut(3, ShortcutType.ACTION, 5),
            InitialShortcut(10, ShortcutType.SKILL, SelfHeal.id),
            InitialShortcut(11, type = ShortcutType.ITEM, TutorialGuide.id)
        ),
        collisionBox = CollisionBox(7.5, 22.8)
    )

    override val skillTree = mapOf(
        1 to listOf(
            SkillToLearn(WindStrike.id, 1, autoLearn = true),
            SkillToLearn(SelfHeal.id, 1, autoLearn = true),
            SkillToLearn(Spellcraft.id, 1, autoLearn = true)
        )
    )
}
