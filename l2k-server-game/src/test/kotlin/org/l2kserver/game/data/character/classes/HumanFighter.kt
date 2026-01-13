package org.l2kserver.game.data.character.classes

import org.l2kserver.game.data.item.armor.SquiresPants
import org.l2kserver.game.data.item.armor.SquiresShirt
import org.l2kserver.game.data.item.book.TutorialGuide
import org.l2kserver.game.data.item.weapons.Dagger
import org.l2kserver.game.data.item.weapons.SquiresSword
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.InitialItem
import org.l2kserver.game.model.actor.character.InitialShortcut
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.actor.character.DefaultEmptySlotStats
import org.l2kserver.game.model.actor.character.ShortcutType
import org.l2kserver.game.model.actor.character.SkillToLearn
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

private val HUMAN_FIGHTER_CP_TABLE = listOf(
    1.0,     32.0,     36.732,   41.516,   46.352,   51.24,   56.18,   61.172,   66.216,   71.312,  //0 - 9
    76.46,   81.66,    86.912,   92.216,   97.572,   102.98,  108.44,  113.952,  119.516,  125.132, //10 - 19
    130.8,   136.52,   142.292,  148.116,  153.992,  159.92,  165.9,   171.932,  178.016,  184.152,
    190.34,  196.58,   202.872,  209.216,  215.612,  222.06,  228.56,  235.112,  241.716,  248.372,
    255.08,  261.84,   268.652,  275.516,  282.432,  289.4,   296.42,  303.492,  310.616,  317.792,
    325.02,  332.3,    339.632,  347.016,  354.452,  361.94,  369.48,  377.072,  384.716,  392.412,
    400.16,  407.96,   415.812,  423.716,  431.672,  439.68,  447.74,  455.852,  464.016,  472.232,
    480.5,   488.82,   497.192,  505.616,  514.092,  522.62,  531.2,   539.832,  548.516,  557.252,
    566.04,  574.88,   583.772,  592.716,  601.712,  610.76,
)

private val HUMAN_FIGHTER_HP_TABLE = listOf(
    1.0,      80.0,     91.83,    103.79,   115.88,   128.1,   140.45,  152.93,   165.54,   178.28,
    191.15,   204.15,   217.28,   230.54,   243.93,   257.45,  271.1,   284.88,   298.79,   312.83,
    327.0,    341.3,    355.73,   370.29,   384.98,   399.8,   414.75,  429.83,   445.04,   460.38,
    475.85,   491.45,   507.18,   523.04,   539.03,   555.15,  571.4,   587.78,   604.29,   620.93,
    637.7,    654.6,    671.63,   688.79,   706.08,   723.5,   741.05,  758.73,   776.54,   794.48,
    812.55,   830.75,   849.08,   867.54,   886.13,   904.85,  923.7,   942.68,   961.79,   981.03,
    1000.4,   1019.9,   1039.53,  1059.29,  1079.18,  1099.2,  1119.35, 1139.63,  1160.04,  1180.58,
    1201.25,  1222.05,  1242.98,  1264.04,  1285.23,  1306.55, 1328.0,  1349.58,  1371.29,  1393.13,
    1415.1,   1437.2,   1459.43,  1481.79,  1504.28,  1526.9
)

private val HUMAN_FIGHTER_MP_TABLE = listOf(
    1.0,      30.0,     35.46,    40.98,    46.56,    52.2,    57.9,    63.66,    69.48,    75.36,
    81.3,     87.3,     93.36,    99.48,    105.66,   111.9,   118.2,   124.56,   130.98,   137.46,
    144.0,    150.6,    157.26,   163.98,   170.76,   177.6,   184.5,   191.46,   198.48,   205.56,
    212.7,    219.9,    227.16,   234.48,   241.86,   249.3,   256.8,   264.36,   271.98,   279.66,
    287.4,    295.2,    303.06,   310.98,   318.96,   327.0,   335.1,   343.26,   351.48,   359.76,
    368.1,    376.5,    384.96,   393.48,   402.06,   410.7,   419.4,   428.16,   436.98,   445.86,
    454.8,    463.8,    472.86,   481.98,   491.16,   500.4,   509.7,   519.06,   528.48,   537.96,
    547.5,    557.1,    566.76,   576.48,   586.26,   596.1,   606.0,   615.96,   625.98,   636.06,
    646.2,    656.4,    666.66,   676.98,   687.36,   697.8,
)

data object HumanFighter: CharacterClass() {
    override val id = 0
    override val requiredLevel = 1

    override val basicStats = BasicStats(
        STR(40),
        DEX(30),
        CON(43),
        INT(21),
        WIT(11),
        MEN(25),
    )

    override val tradeAndInventoryStats = TradeAndInventoryStats(privateStoreSize = 4)

    override val emptySlotStats = DefaultEmptySlotStats.FIGHTER

    override val parentClass = null

    override val baseAtkSpd = 300
    override val baseSpeed = 115

    override val characterTemplate = CharacterTemplate(
        position = Position(-71338, 258271, -3104),
        items = listOf(
            InitialItem(SquiresShirt.id, isEquipped = true),
            InitialItem(SquiresPants.id, isEquipped = true),
            InitialItem(SquiresSword.id, isEquipped = true),
            InitialItem(Dagger.id, isEquipped = false),
            InitialItem(TutorialGuide.id, isEquipped = false)
        ),
        shortcuts = listOf(
            InitialShortcut(0, ShortcutType.ACTION, 2),
            InitialShortcut(3, ShortcutType.ACTION, 5),
            InitialShortcut(10, ShortcutType.ACTION, 0),
            InitialShortcut(11, type = ShortcutType.ITEM, TutorialGuide.id)
        ),
        collisionBox = CollisionBox(9.0, 23.0)
    )

    override fun getCombatStats(characterLevel: Int) = CombatStats(
        maxCp = HUMAN_FIGHTER_CP_TABLE[characterLevel],
        maxHp = HUMAN_FIGHTER_HP_TABLE[characterLevel],
        maxMp = HUMAN_FIGHTER_MP_TABLE[characterLevel],

        speed = 115,
        castingSpd = 333,

        cpRegen = DEFAULT_CP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10],
        hpRegen = DEFAULT_HP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10],
        mpRegen = DEFAULT_MP_REGEN_PER_10_LEVELS[(characterLevel - 1) / 10]
    )

    override val skillTree: Map<Int, List<SkillToLearn>> = emptyMap() //TODO Lucky and Common Craft skills

}
