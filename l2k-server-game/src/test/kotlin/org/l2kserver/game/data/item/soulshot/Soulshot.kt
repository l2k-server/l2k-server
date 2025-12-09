package org.l2kserver.game.data.item.soulshot

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.SoulshotTemplate

object SoulshotNoGrade: SoulshotTemplate() {
    override val id = 1835
    override val name = "Soulshot: No Grade"
    override val grade = Grade.NO_GRADE
    override val weight = 4
    override val price = 7
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
}

object SoulshotSGrade: SoulshotTemplate() {
    override val id = 1467
    override val name = "Soulshot: S-Grade"
    override val grade = Grade.S
    override val weight = 2
    override val price = 100
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
}
