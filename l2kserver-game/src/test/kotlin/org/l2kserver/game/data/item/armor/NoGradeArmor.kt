package org.l2kserver.game.data.item.armor

import org.l2kserver.game.model.item.ArmorTemplate
import org.l2kserver.game.model.item.ArmorType
import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.stats.Stats

val LEATHER_SHIELD = ArmorTemplate(
    id = 18,
    name = "Leather Shield",
    grade = Grade.NO_GRADE,
    weight = 1430,
    price = 39,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    type = ArmorType.SHIELD,
    stats = Stats(
        shieldDef = 47,
        shieldDefRate = 20,
        evasion = -8
    ),
    crystalCount = 0
)

val SQUIRES_SHIRT = ArmorTemplate(
    id = 1146,
    name = "Squire's Shirt",
    grade = Grade.NO_GRADE,
    weight = 3301,
    price = 26,
    isSellable = false,
    isDroppable = false,
    isDestroyable = true,
    isExchangeable = false,
    type = ArmorType.UPPER_BODY_LIGHT,
    stats = Stats(
        pDef = 33
    ),
    crystalCount = 0
)

val SQUIRES_PANTS = ArmorTemplate(
    id = 1147,
    name = "Squire's Pants",
    grade = Grade.NO_GRADE,
    weight = 1750,
    price = 6,
    isSellable = false,
    isDroppable = false,
    isDestroyable = true,
    isExchangeable = false,
    type = ArmorType.LOWER_BODY_LIGHT,
    stats = Stats(
        pDef = 20
    ),
    crystalCount = 0
)
