package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.stats.TradeAndInventoryStats

/**
 * Character class data - stats, skills, misc. information
 *
 * @property id Class identifier
 * @property requiredLevel Level, required to take this class. IMPORTANT for proper stats calculation
 * @property combatStats Initial values of character's basic stats
 * @property basicStats Initial values of character's stats
 * @property tradeAndInventoryStats Initial values for character trading stats
 * @property emptySlotStats Stats of character's empty slots. Will be applied if no item equipped in slot
 * @property perLevelGain CP, HP and MP per level gain coefficients
 * @property parentClass Parent class. For example, in L2 for 'Duelist' class parent class will be 'Gladiator'
 * @property characterTemplate Template for this class character creation
 */
data class L2kCharacterClass(
    override val id: Int,
    val requiredLevel: Int,
    val combatStats: Stats,
    val basicStats: BasicStats,
    val tradeAndInventoryStats: TradeAndInventoryStats,
    val emptySlotStats: Map<Slot, Stats>,
    val perLevelGain: PerLevelGain,
    val parentClass: L2kCharacterClass? = null,
    val characterTemplate: CharacterTemplate? = null
): GameData {

    /**
     * Returns base class identifier.
     * For example, in L2 for 'Duelist' class base class will be 'Human Fighter'
     */
    val baseClassId: Int get() = if (this.parentClass == null) id else this.parentClass.baseClassId

    val baseAtkSpd = emptySlotStats.values.reduce { acc, stats -> acc + stats }.atkSpd
    val baseSpeed = combatStats.speed

    object Registry: GameDataRegistry<L2kCharacterClass>()
}

/**`
 * Coefficients for max CP, HP and MP calculation according to character's level
 */
data class PerLevelGain(
    val cpAdd: Double = 0.0,
    val cpMod: Double = 0.0,
    val hpAdd: Double = 0.0,
    val hpMod: Double = 0.0,
    val mpAdd: Double = 0.0,
    val mpMod: Double = 0.0
)

//TODO https://github.com/orgs/l2kserver/projects/1/views/3?pane=issue&itemId=122600531&issue=l2kserver%7Cl2kserver%7C36
enum class CharacterClassName(val id: Int) {
    HUMAN_FIGHTER(0),
    WARRIOR(1),
    GLADIATOR(2),
    WARLORD(3),
    HUMAN_KNIGHT(4),
    PALADIN(5),
    DARK_AVENGER(6),
    ROGUE(7),
    TREASURE_HUNTER(8),
    HAWKEYE(9),

    HUMAN_MYSTIC(10),
    HUMAN_WIZARD(11),
    SORCERER(12),
    NECROMANCER(13),
    WARLOCK(14),
    CLERIC(15),
    BISHOP(16),
    PROPHET(17),

    ELVEN_FIGHTER(18),
    ELVEN_KNIGHT(19),
    TEMPLE_KNIGHT(20),
    SWORDSINGER(21),
    ELVEN_SCOUT(22),
    PLAINSWALKER(23),
    SILVER_RANGER(24),

    ELVEN_MYSTIC(25),
    ELVEN_WIZARD(26),
    SPELLSINGER(27),
    ELEMENTAL_SUMMONER(28),
    ELVEN_ORACLE(29),
    ELVEN_ELDER(30),

    DARK_FIGHTER(31),
    PALUS_KNIGHT(32),
    SHILLIEN_KNIGHT(33),
    BLADEDANCER(34),
    ASSASSIN(35),
    ABYSS_WALKER(36),
    PHANTOM_RANGER(37),

    DARK_MYSTIC(38),
    DARK_WIZARD(39),
    SPELLHOWLER(40),
    PHANTOM_SUMMONER(41),
    SHILLIEN_ORACLE(42),
    SHILLIEN_ELDER(43),

    ORC_FIGHTER(44),
    ORC_RAIDER(45),
    DESTROYER(46),
    MONK(47),
    TYRANT(48),

    ORC_MYSTIC(49),
    ORC_SHAMAN(50),
    OVERLORD(51),
    WARCRYER(52),

    DWARVEN_FIGHTER(53),
    SCAVENGER(54),
    BOUNTY_HUNTER(55),
    ARTISAN(56),
    WARSMITH(57),

    DUELIST(88),
    DREADNOUGHT(89),
    PHOENIX_KNIGHT(90),
    HELL_KNIGHT(91),
    SAGITTARIUS(92),
    ADVENTURER(93),

    ARCHMAGE(94),
    SOULTAKER(95),
    ARCANA_LORD(96),
    CARDINAL(97),
    HIEROPHANT(98),

    EVAS_TEMPLAR(99),
    SWORD_MUSE(100),
    WIND_RIDER(101),
    MOONLIGHT_SENTINEL(102),

    MYSTIC_MUSE(103),
    ELEMENTAL_MASTER(104),
    EVAS_SAINT(105),

    SHILLIEN_TEMPLAR(106),
    SPECTRAL_DANCER(107),
    GHOST_HUNTER(108),
    GHOST_SENTINEL(109),

    STORM_SCREAMER(110),
    SPECTRAL_MASTER(111),
    SHILLIEN_SAINT(112),

    TITAN(113),
    GRAND_KHAVATARI(114),

    DOMINATOR(115),
    DOOMCRYER(116),

    FORTUNE_SEEKER(117),
    MAESTRO(118);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid character class id '$id'" }
    }
}
