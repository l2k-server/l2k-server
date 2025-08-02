CREATE TYPE GENDER AS ENUM ('MALE', 'FEMALE');
CREATE TYPE RACE AS ENUM ('HUMAN', 'ELF', 'DARK_ELF', 'ORC', 'DWARF');
CREATE TYPE ACCESS_LEVEL AS ENUM('PLAYER', 'GAME_MASTER');

CREATE TABLE characters(
    account_name               VARCHAR(16) NOT NULL,
    name                       VARCHAR(16) UNIQUE NOT NULL,
    title                      VARCHAR(16) NOT NULL,
    clan_id                    INT NOT NULL,
    gender                     GENDER NOT NULL,
    race                       RACE NOT NULL,
    class_id                   INT NOT NULL,
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
