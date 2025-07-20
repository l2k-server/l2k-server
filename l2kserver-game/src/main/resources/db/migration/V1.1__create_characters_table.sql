
CREATE TYPE GENDER AS ENUM ('MALE', 'FEMALE');

CREATE TYPE RACE AS ENUM ('HUMAN', 'ELF', 'DARK_ELF', 'ORC', 'DWARF');

CREATE TYPE CHARACTER_CLASS AS ENUM (
    -- Human Fighter classes --
    'HUMAN_FIGHTER',
    -- 1st classes
    'WARRIOR', 'HUMAN_KNIGHT', 'ROGUE',
    -- 2nd classes
    'GLADIATOR', 'WARLORD', 'PALADIN', 'DARK_AVENGER', 'TREASURE_HUNTER', 'HAWKEYE',
    -- 3rd classes
    'DREADNOUGHT', 'DUELIST', 'PHOENIX_KNIGHT', 'HELL_KNIGHT', 'ADVENTURER','SAGITTARIUS',

    -- Human Mystic classes
    'HUMAN_MYSTIC',
    -- 1st classes
    'HUMAN_WIZARD', 'CLERIC',
    -- 2nd classes
    'SORCERER', 'NECROMANCER', 'WARLOCK', 'BISHOP', 'PROPHET',
    -- 3rd classes
    'ARCHMAGE', 'SOULTAKER', 'ARCANA_LORD', 'CARDINAL', 'HIEROPHANT',

    -- Elven Fighter classes
    'ELVEN_FIGHTER',
    -- 1st classes
    'ELVEN_KNIGHT', 'ELVEN_SCOUT',
    -- 2nd classes
    'TEMPLE_KNIGHT', 'SWORDSINGER', 'PLAINSWALKER', 'SILVER_RANGER',
    -- 3rd classes
    'EVAS_TEMPLAR', 'SWORD_MUSE', 'WIND_RIDER', 'MOONLIGHT_SENTINEL',

    -- Elven Mystic classes
    'ELVEN_MYSTIC',
    -- 1st classes
    'ELVEN_WIZARD', 'ELVEN_ORACLE',
    -- 2nd classes
    'SPELLSINGER', 'ELEMENTAL_SUMMONER', 'ELVEN_ELDER',
    -- 3rd classes
    'MYSTIC_MUSE', 'ELEMENTAL_MASTER', 'EVAS_SAINT',

    -- Dark Elf Fighter classes
    'DARK_FIGHTER',
    -- 1st classes
    'PALUS_KNIGHT', 'ASSASSIN',
    -- 2nd classes
    'SHILLIEN_KNIGHT', 'BLADEDANCER', 'ABYSS_WALKER', 'PHANTOM_RANGER',
    -- 3rd classes
    'SHILLIEN_TEMPLAR', 'SPECTRAL_DANCER', 'GHOST_HUNTER', 'GHOST_SENTINEL',

    -- Dark Elf Mystic classes
    'DARK_MYSTIC',
    -- 1st classes
    'DARK_WIZARD', 'SHILLIEN_ORACLE',
    -- 2nd classes
    'SPELLHOWLER',  'PHANTOM_SUMMONER', 'SHILLIEN_ELDER',
    -- 3rd classes
    'STORM_SCREAMER', 'SPECTRAL_MASTER', 'SHILLIEN_SAINT',

    -- Orc Fighter classes
    'ORC_FIGHTER',
    -- 1st classes
    'ORC_RAIDER', 'MONK',
    -- 2nd classes
    'DESTROYER', 'TYRANT',
    -- 3rd classes
    'TITAN', 'GRAND_KHAVATARI',

    -- Orc Mystic classes
    'ORC_MYSTIC',
    -- 1st classes
    'ORC_SHAMAN',
    -- 2nd classes
    'OVERLORD', 'WARCRYER',
    -- 3rd classes
    'DOMINATOR', 'DOOMCRYER',

    -- Dwarven Fighter classes
    'DWARVEN_FIGHTER',
    -- 1st classes
    'SCAVENGER', 'ARTISAN',
    -- 2nd classes
    'BOUNTY_HUNTER', 'WARSMITH',
    -- 3rd classes
    'FORTUNE_SEEKER', 'MAESTRO'
);

CREATE TYPE ACCESS_LEVEL AS ENUM('PLAYER', 'GAME_MASTER');

CREATE TABLE characters(
    account_name               VARCHAR(16) NOT NULL,
    name                       VARCHAR(16) UNIQUE NOT NULL,
    title                      VARCHAR(16) NOT NULL,
    clan_id                    INT NOT NULL,
    gender                     GENDER NOT NULL,
    race                       RACE NOT NULL,
    class_name                 CHARACTER_CLASS NOT NULL,
    current_hp                 INT NOT NULL,
    current_mp                 INT NOT NULL,
    current_cp                 INT NOT NULL,
    sp                         INT NOT NULL,
    exp                        BIGINT NOT NULL,
    karma                      INT NOT NULL,
    pvp_count                  INT NOT NULL,
    pk_count                   INT NOT NULL,
    hair_style                 INT NOT NULL,
    hair_color                 INT NOT NULL,
    face_type                  INT NOT NULL,
    last_access                TIMESTAMP NOT NULL,
    deletion_date              TIMESTAMP,
    x                          INT NOT NULL,
    y                          INT NOT NULL,
    z                          INT NOT NUll,
    name_color                 INT NOT NULL,
    title_color                INT NOT NULL,
    active_subclass            SMALLINT NOT NULL,
    access_level               ACCESS_LEVEL NOT NULL,
    id                         INT NOT NULL DEFAULT nextval('id_sequence') PRIMARY KEY
);
