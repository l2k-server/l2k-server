package org.l2kserver.game.data.characterclass

import org.l2kserver.game.data.item.armor.ApprenticesStockings
import org.l2kserver.game.data.item.armor.ApprenticeTunic
import org.l2kserver.game.data.item.book.TutorialGuide
import org.l2kserver.game.data.item.weapon.ApprenticesWand
import org.l2kserver.game.data.skill.SelfHeal
import org.l2kserver.game.data.skill.WindStrike
import org.l2kserver.game.data.skill.Spellcraft
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.DefaultEmptySlotStats
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
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

private val HUMAN_MYSTIC_HP_TABLE = listOf(
    1.0,     101.0,   116.47,  132.11,  147.92,  163.9,   180.05,  196.37,  212.86,  229.52,
    246.35,  263.35,  280.52,  297.86,  315.37,  333.05,  350.9,   368.92,  387.11,  405.47,
    424.0,   442.7,   461.57,  480.61,  499.82,  519.2,   538.75,  558.47,  578.36,  598.42,
    618.65,  639.05,  659.62,  680.36,  701.27,  722.35,  743.6,   765.02,  786.61,  808.37,
    830.3,   852.4,   874.67,  897.11,  919.72,  942.5,   965.45,  988.57,  1011.86, 1035.32,
    1058.95, 1082.75, 1106.72, 1130.86, 1155.17, 1179.65, 1204.3,  1229.12, 1254.11, 1279.27,
    1304.6,  1330.1,  1355.77, 1381.61, 1407.62, 1433.8,  1460.15, 1486.67, 1513.36, 1540.22,
    1567.25, 1594.45, 1621.82, 1649.36, 1677.07, 1704.95, 1733.0,  1761.22, 1789.61, 1818.17,
    1846.9,
)

private val HUMAN_MYSTIC_MP_TABLE = listOf(
    1.0,     40.0,    47.28,   54.64,   62.08,   69.6,    77.2,    84.88,   92.64,   100.48,
    108.4,   116.4,   124.48,  132.64,  140.88,  149.2,   157.6,   166.08,  174.64,  183.28,
    192.0,   200.8,   209.68,  218.64,  227.68,  236.8,   246.0,   255.28,  264.64,  274.08,
    283.6,   293.2,   302.88,  312.64,  322.48,  332.4,   342.4,   352.48,  362.64,  372.88,
    383.2,   393.6,   404.08,  414.64,  425.28,  436.0,   446.8,   457.68,  468.64,  479.68,
    490.8,   502.0,   513.28,  524.64,  536.08,  547.6,   559.2,   570.88,  582.64,  594.48,
    606.4,   618.4,   630.48,  642.64,  654.88,  667.2,   679.6,   692.08,  704.64,  717.28,
    730.0,   742.8,   755.68,  768.64,  781.68,  794.8,   808.0,   821.28,  834.64,  848.08,
    861.6,
)

private val HUMAN_MYSTIC_CP_TABLE = listOf(
    1.0,     50.5,    58.235,  66.055,  73.96,   81.95,   90.025,  98.185,  106.43,  114.76,
    123.175, 131.675, 140.26,  148.93,  157.685, 166.525, 175.45,  184.46,  193.555, 202.735,
    212.0,   221.35,  230.785, 240.305, 249.91,  259.6,   269.375, 279.235, 289.18,  299.21,
    309.325, 319.525, 329.81,  340.18,  350.635, 361.175, 371.8,   382.51,  393.305, 404.185,
    415.15,  426.2,   437.335, 448.555, 459.86,  471.25,  482.725, 494.285, 505.93,  517.66,
    529.475, 541.375, 553.36,  565.43,  577.585, 589.825, 602.15,  614.56,  627.055, 639.635,
    652.3,   665.05,  677.885, 690.805, 703.81,  716.9,   730.075, 743.335, 756.68,  770.11,
    783.625, 797.225, 810.91,  824.68,  838.535, 852.475, 866.5,   880.61,  894.805, 909.085,
    923.45,
)

data object HumanMystic: CharacterClass() {
    override val id = 10
    override val requiredLevel = 1
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
    override val baseAtkSpd = 300
    override val baseSpeed = 120

    override fun getCombatStats(characterLevel: Int) = CombatStats(
        maxCp = HUMAN_MYSTIC_CP_TABLE[characterLevel],
        maxHp = HUMAN_MYSTIC_HP_TABLE[characterLevel],
        maxMp = HUMAN_MYSTIC_MP_TABLE[characterLevel],

        speed = 120,
        castingSpd = 333,

        cpRegen = DEFAULT_CP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10],
        hpRegen = DEFAULT_HP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10],
        mpRegen = DEFAULT_MP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10]
    )
}
