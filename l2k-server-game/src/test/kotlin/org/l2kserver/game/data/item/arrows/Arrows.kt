package org.l2kserver.game.data.item.arrows

import org.l2kserver.game.model.item.template.ArrowTemplate
import org.l2kserver.game.model.item.template.Grade

object WoodenArrow: ArrowTemplate() {
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

object BoneArrow: ArrowTemplate() {
    override val id = 1341
    override val name = "Bone Arrow"
    override val grade = Grade.NO_GRADE
    override val weight = 5
    override val price = 3
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
}
