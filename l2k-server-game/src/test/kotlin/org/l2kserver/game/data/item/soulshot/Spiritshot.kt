package org.l2kserver.game.data.item.soulshot

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.SpiritshotTemplate
import org.l2kserver.game.model.item.template.SpiritshotType

val SPIRITSHOT_NO_GRADE = SpiritshotTemplate(
    id = 2509,
    name = "Spiritshot: No Grade",
    grade = Grade.NO_GRADE,
    weight = 5,
    price = 15,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    spiritshotType = SpiritshotType.SPIRITSHOT
)

val BLESSED_SPIRITSHOT_NO_GRADE = SpiritshotTemplate(
    id = 3947,
    name = "Blessed Spiritshot: No Grade",
    grade = Grade.NO_GRADE,
    weight = 5,
    price = 35,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    spiritshotType = SpiritshotType.BLESSED_SPIRITSHOT
)
