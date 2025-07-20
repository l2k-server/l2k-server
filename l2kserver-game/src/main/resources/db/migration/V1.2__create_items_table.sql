CREATE TYPE EQUIPPED_AT AS ENUM (
    'UNDERWEAR',
    'RIGHT_EARRING','LEFT_EARRING',
    'NECKLACE',
    'RIGHT_RING', 'LEFT_RING',
    'HEADGEAR',
    'RIGHT_HAND',
    'LEFT_HAND',
    'GLOVES',
    'UPPER_BODY',
    'LOWER_BODY',
    'BOOTS',
    'TWO_HANDS',
    'UPPER_AND_LOWER_BODY',
    'HAIR_ACCESSORY',
    'FACE_ACCESSORY',
    'TWO_SLOT_ACCESSORY'
);

CREATE TABLE items(
    template_id                INT NOT NULL,
    owner_id                   INT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    amount                     INT NOT NULL CONSTRAINT positive_amount CHECK (amount > 0),
    equipped_at                EQUIPPED_AT,
    enchant_level              INT NOT NULL CONSTRAINT positive_enchant_level CHECK (enchant_level >= 0),
    augmentation_id            INT NOT NULL,
    id                         INT NOT NULL DEFAULT nextval('id_sequence') PRIMARY KEY
);

CREATE INDEX items_owner_id_index ON items(owner_id) WITH (deduplicate_items = off);
