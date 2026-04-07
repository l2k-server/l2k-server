package org.l2kserver.game.data.item.arrow

import org.l2kserver.game.model.item.template.Arrow
import org.l2kserver.game.model.item.template.Grade

data object WoodenArrow: Arrow() {
    override val id = 17
    override val name = "Wooden Arrow"
    override val grade = Grade.NO_GRADE
    override val weight = 6
    override val price = 2
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
}

data object BoneArrow: Arrow() {
    override val id = 1341
    override val name = "Bone Arrow"
    override val grade = Grade.D
    override val weight = 5
    override val price = 3
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
}
