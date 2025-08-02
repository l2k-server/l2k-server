package org.l2kserver.game.data.character

import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterClassName
import org.l2kserver.game.model.actor.character.PerLevelGain
import org.l2kserver.game.model.item.Slot
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CON
import org.l2kserver.game.model.stats.DEX
import org.l2kserver.game.model.stats.INT
import org.l2kserver.game.model.stats.MEN
import org.l2kserver.game.model.stats.STR
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.model.stats.WIT

val HUMAN_FIGHTER_CLASS = CharacterClass(
    name = CharacterClassName.HUMAN_FIGHTER,
    requiredLevel = 1, //IMPORTANT for proper stats calculation
    parentClassName = null,
    initialBasicStats = BasicStats(
        STR(40),
        DEX(30),
        CON(43),
        INT(21),
        WIT(11),
        MEN(25),
    ),
    initialStats = Stats(
        maxCp = 32,
        maxHp = 80,
        maxMp = 30,

        mDef = 41,
        speed = 115,
        castingSpd = 333,

        hpRegen = 1.5,
        mpRegen = 0.9,
        cpRegen = 1.5,
    ),
    initialTradeAndInventoryStats = TradeAndInventoryStats(
        privateStoreSize = 4
    ),
    emptySlotStats = mapOf(
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
        Slot.BOOTS to Stats(pDef = 7)
    ),
    perLevelGain = PerLevelGain(
        cpAdd = 4.73,
        cpMod = 0.22,
        hpAdd = 11.83,
        hpMod = 0.37,
        mpAdd = 5.46,
        mpMod = 0.14,
    )
)
