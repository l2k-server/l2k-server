package org.l2kserver.game.data.item.etc

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.SimpleItemTemplate

val ADENA = SimpleItemTemplate(
    id = 57,
    name = "Adena",
    grade = Grade.NO_GRADE,
    weight = 0,
    price = 1,
    isSellable = false,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    isStackable = true
)
