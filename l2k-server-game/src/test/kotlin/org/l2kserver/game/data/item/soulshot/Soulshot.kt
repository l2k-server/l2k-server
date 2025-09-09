package org.l2kserver.game.data.item.soulshot

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.SoulshotTemplate

val SOULSHOT_NO_GRADE = SoulshotTemplate(
    id = 1835,
    name = "Soulshot: No Grade",
    grade = Grade.NO_GRADE,
    weight = 4,
    price = 7,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true
)

val SOULSHOT_S_GRADE = SoulshotTemplate(
    id = 1467,
    name = "Soulshot: S-Grade",
    grade = Grade.S,
    weight = 2,
    price = 100,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true
)
