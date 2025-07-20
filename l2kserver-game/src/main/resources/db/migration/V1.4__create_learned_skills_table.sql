CREATE TABLE learned_skills(
    character_id               INT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    subclass_index             INT NOT NULL,
    skill_id                   INT NOT NULL,
    skill_level                INT NOT NULL,
    id                         SERIAL PRIMARY KEY,
    -- TODO skill enchantment

    UNIQUE (character_id, skill_id, subclass_index)
);

CREATE INDEX learned_skills_character_and_subclass_index
    ON learned_skills(character_id, subclass_index)
    WITH (deduplicate_items = off);
