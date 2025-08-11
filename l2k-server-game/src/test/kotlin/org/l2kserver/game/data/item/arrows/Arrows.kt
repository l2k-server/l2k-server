package org.l2kserver.game.data.item.arrows

import org.l2kserver.game.model.item.ArrowTemplate
import org.l2kserver.game.model.item.Grade

val WOODEN_ARROW = ArrowTemplate(
    id = 17,
    name = "Wooden Arrow",
    grade = Grade.NO_GRADE,
    weight = 6,
    price = 2,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    isStackable = true
)

val BONE_ARROW = ArrowTemplate(
    id = 1341,
    name = "Bone Arrow",
    grade = Grade.NO_GRADE,
    weight = 5,
    price = 3,
    isSellable = true,
    isDroppable = true,
    isDestroyable = true,
    isExchangeable = true,
    isStackable = true
)
