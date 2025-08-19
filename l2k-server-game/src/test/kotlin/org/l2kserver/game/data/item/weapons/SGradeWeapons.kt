package org.l2kserver.game.data.item.weapons

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.WeaponTemplate
import org.l2kserver.game.model.item.template.WeaponType
import org.l2kserver.game.model.stats.Stats

val DEMON_SPLINTER = WeaponTemplate(
    id = 6371,
    name = "Demon Splinter",
    grade = Grade.S,
    weight = 1350,
    price = 48_800_000,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.FIST,
    stats = Stats(
        pAtk = 342,
        mAtk = 132,
        atkSpd = 325,
        critRate = 40
    ),
    crystalCount = 2_440,
    soulshotUsed = 1,
    spiritshotUsed = 1
)

val HEAVENS_DIVIDER = WeaponTemplate(
    id = 6372,
    name = "Heaven's Divider",
    grade = Grade.S,
    weight = 1380,
    price = 48_800_000,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.SWORD_TWO_HANDED,
    stats = Stats(
        pAtk = 342,
        mAtk = 132,
        atkSpd = 325,
        critRate = 80
    ),
    crystalCount = 2_440,
    soulshotUsed = 1,
    spiritshotUsed = 1
)

val TALLUM_BLADE_DARK_LEGIONS_EDGE = WeaponTemplate(
    id = 6580,
    name = "Tallum Blade*Dark Legion's Edge",
    grade = Grade.S,
    weight = 2080,
    price = 48_800_000,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = WeaponType.DOUBLE_BLADES,
    stats = Stats(
        pAtk = 342,
        mAtk = 132,
        atkSpd = 325,
        critRate = 80
    ),
    crystalCount = 2_440,
    soulshotUsed = 1,
    spiritshotUsed = 1
)
