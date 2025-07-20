package org.l2kserver.game.model.actor.enumeration

import com.fasterxml.jackson.annotation.JsonProperty

enum class CharacterClassName(val id: Int) {
    @JsonProperty("Human Fighter") HUMAN_FIGHTER(0),
    @JsonProperty("Warrior") WARRIOR(1),
    @JsonProperty("Gladiator") GLADIATOR(2),
    @JsonProperty("Warlord") WARLORD(3),
    @JsonProperty("Human Knight") HUMAN_KNIGHT(4),
    @JsonProperty("Paladin") PALADIN(5),
    @JsonProperty("Dark Avenger") DARK_AVENGER(6),
    @JsonProperty("Rogue") ROGUE(7),
    @JsonProperty("Treasure Hunter") TREASURE_HUNTER(8),
    @JsonProperty("Hawkeye") HAWKEYE(9),

    @JsonProperty("Human Mystic") HUMAN_MYSTIC(10),
    @JsonProperty("Human Wizard")HUMAN_WIZARD(11),
    @JsonProperty("Sorcerer") SORCERER(12),
    @JsonProperty("Necromancer") NECROMANCER(13),
    @JsonProperty("Warlock") WARLOCK(14),
    @JsonProperty("Cleric") CLERIC(15),
    @JsonProperty("Bishop") BISHOP(16),
    @JsonProperty("Prophet") PROPHET(17),

    @JsonProperty("Elven Fighter") ELVEN_FIGHTER(18),
    @JsonProperty("Elven Knight") ELVEN_KNIGHT(19),
    @JsonProperty("Temple Knight") TEMPLE_KNIGHT(20),
    @JsonProperty("Swordsinger") SWORDSINGER(21),
    @JsonProperty("Elven Scout") ELVEN_SCOUT(22),
    @JsonProperty("Plainswalker") PLAINSWALKER(23),
    @JsonProperty("Silver Ranger") SILVER_RANGER(24),

    @JsonProperty("Elven Mystic") ELVEN_MYSTIC(25),
    @JsonProperty("Elven Wizard") ELVEN_WIZARD(26),
    @JsonProperty("Spellsinger") SPELLSINGER(27),
    @JsonProperty("Elemental Summoner") ELEMENTAL_SUMMONER(28),
    @JsonProperty("Elven Oracle") ELVEN_ORACLE(29),
    @JsonProperty("Elven Elder") ELVEN_ELDER(30),

    @JsonProperty("Dark Fighter") DARK_FIGHTER(31),
    @JsonProperty("Palus Knight") PALUS_KNIGHT(32),
    @JsonProperty("Shillien Knight") SHILLIEN_KNIGHT(33),
    @JsonProperty("Bladedancer") BLADEDANCER(34),
    @JsonProperty("Assassin") ASSASSIN(35),
    @JsonProperty("Abyss Walker") ABYSS_WALKER(36),
    @JsonProperty("Phantom Ranger") PHANTOM_RANGER(37),

    @JsonProperty("Dark Mystic") DARK_MYSTIC(38),
    @JsonProperty("Dark Wizard") DARK_WIZARD(39),
    @JsonProperty("Spellhowler") SPELLHOWLER(40),
    @JsonProperty("Phantom Summoner") PHANTOM_SUMMONER(41),
    @JsonProperty("Shillien Oracle") SHILLIEN_ORACLE(42),
    @JsonProperty("Shillien Elder") SHILLIEN_ELDER(43),

    @JsonProperty("Orc Fighter") ORC_FIGHTER(44),
    @JsonProperty("Orc Raider") ORC_RAIDER(45),
    @JsonProperty("Destroyer") DESTROYER(46),
    @JsonProperty("Monk") MONK(47),
    @JsonProperty("Tyrant") TYRANT(48),

    @JsonProperty("Orc Mystic") ORC_MYSTIC(49),
    @JsonProperty("Orc Shaman") ORC_SHAMAN(50),
    @JsonProperty("Overlord") OVERLORD(51),
    @JsonProperty("Warcryer") WARCRYER(52),

    @JsonProperty("Dwarven Fighter") DWARVEN_FIGHTER(53),
    @JsonProperty("Scavenger") SCAVENGER(54),
    @JsonProperty("Bounty Hunter") BOUNTY_HUNTER(55),
    @JsonProperty("Artisan") ARTISAN(56),
    @JsonProperty("Warsmith") WARSMITH(57),

    @JsonProperty("Duelist") DUELIST(88),
    @JsonProperty("Dreadnought") DREADNOUGHT(89),
    @JsonProperty("Phoenix Knight") PHOENIX_KNIGHT(90),
    @JsonProperty("Hell Knight") HELL_KNIGHT(91),
    @JsonProperty("Sagittarius") SAGITTARIUS(92),
    @JsonProperty("Adventurer") ADVENTURER(93),

    @JsonProperty("Archmage") ARCHMAGE(94),
    @JsonProperty("Soultaker") SOULTAKER(95),
    @JsonProperty("Arcana Lord") ARCANA_LORD(96),
    @JsonProperty("Cardinal") CARDINAL(97),
    @JsonProperty("Hierophant") HIEROPHANT(98),

    @JsonProperty("Eva's Templar") EVAS_TEMPLAR(99),
    @JsonProperty("Sword Muse") SWORD_MUSE(100),
    @JsonProperty("Wind Rider") WIND_RIDER(101),
    @JsonProperty("Moonlight Sentinel") MOONLIGHT_SENTINEL(102),

    @JsonProperty("Mystic Muse") MYSTIC_MUSE(103),
    @JsonProperty("Elemental Master") ELEMENTAL_MASTER(104),
    @JsonProperty("Eva's Saint") EVAS_SAINT(105),

    @JsonProperty("Shillien Templar") SHILLIEN_TEMPLAR(106),
    @JsonProperty("Spectral Dancer") SPECTRAL_DANCER(107),
    @JsonProperty("Ghost Hunter") GHOST_HUNTER(108),
    @JsonProperty("Ghost Sentinel") GHOST_SENTINEL(109),

    @JsonProperty("Storm Screamer") STORM_SCREAMER(110),
    @JsonProperty("Spectral Master") SPECTRAL_MASTER(111),
    @JsonProperty("Shillien Saint") SHILLIEN_SAINT(112),

    @JsonProperty("Titan") TITAN(113),
    @JsonProperty("Grand Khavatari") GRAND_KHAVATARI(114),

    @JsonProperty("Dominator") DOMINATOR(115),
    @JsonProperty("Doomcryer") DOOMCRYER(116),

    @JsonProperty("Fortune Seeker") FORTUNE_SEEKER(117),
    @JsonProperty("Maestro") MAESTRO(118);

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.id == id }) { "Invalid character class id '$id'" }
    }
}
