CREATE TYPE SHORTCUT_TYPE AS ENUM (
    'ITEM',
    'SKILL',
    'ACTION',
    'MACRO',
    'RECIPE'
);

CREATE TABLE shortcuts(
    character_id               INT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    index                      SMALLINT NOT NULL,
    type                       SHORTCUT_TYPE NOT NULL,
    shortcut_action_id         INT NOT NULL,
    action_level               SMALLINT NOT NULL,
    subclass_index             SMALLINT NOT NULL,
    id                         SERIAL PRIMARY KEY,

    UNIQUE (character_id, index, subclass_index)
);

CREATE INDEX shortcuts_character_and_subclass_index ON shortcuts(character_id, subclass_index) WITH (deduplicate_items = off);
