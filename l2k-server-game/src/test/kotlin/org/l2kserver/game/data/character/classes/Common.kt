package org.l2kserver.game.data.character.classes

import org.l2kserver.game.model.item.template.Slot
import org.l2kserver.game.model.stats.Stats

val FIGHTER_EMPTY_SLOT_STATS = mapOf(
    Slot.RIGHT_HAND to Stats(
        pAtk = 4,
        mAtk = 6,
        critRate = 44,
        atkSpd = 300,
    ),

    Slot.HEADGEAR to Stats(pDef = 12),
    Slot.UPPER_BODY to Stats(pDef = 31),
    Slot.LOWER_BODY to Stats(pDef = 18),
    Slot.GLOVES to Stats(pDef = 8),
    Slot.BOOTS to Stats(pDef = 7),

    Slot.RIGHT_RING to Stats(mDef = 5),
    Slot.LEFT_RING to Stats(mDef = 5),
    Slot.RIGHT_EARRING to Stats(mDef = 9),
    Slot.LEFT_EARRING to Stats(mDef = 9),
    Slot.NECKLACE to Stats(mDef = 13),
)
