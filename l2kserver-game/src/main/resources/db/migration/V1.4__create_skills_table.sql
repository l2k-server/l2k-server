CREATE TABLE skills(
    character_id               INT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    subclass_index             INT NOT NULL,
    skill_id                   INT NOT NULL,
    skill_level                INT NOT NULL,
    next_usage_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id                         SERIAL PRIMARY KEY,
    -- TODO skill enchantment

    UNIQUE (character_id, skill_id, subclass_index)
);

CREATE INDEX skills_character_id_index ON skills(character_id) WITH (deduplicate_items = off);
