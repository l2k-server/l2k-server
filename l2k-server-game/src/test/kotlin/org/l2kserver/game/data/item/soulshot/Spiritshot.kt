package org.l2kserver.game.data.item.soulshot

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.Spiritshot
import org.l2kserver.game.model.item.template.SpiritshotType

data object SpiritshotNoGrade: Spiritshot() {
    override val id = 2509
    override val name = "Spiritshot: No Grade"
    override val grade = Grade.NO_GRADE
    override val weight = 5
    override val price = 15
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val spiritshotType = SpiritshotType.SPIRITSHOT
}

data object BlessedSpiritshotNoGrade: Spiritshot() {
    override val id = 3947
    override val name = "Blessed Spiritshot: No Grade"
    override val grade = Grade.NO_GRADE
    override val weight = 5
    override val price = 35
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val spiritshotType = SpiritshotType.BLESSED_SPIRITSHOT
}
