package org.l2kserver.game.data.item.etc

import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.item.Item

data object Adena: Item {
    override val id = 57
    override val name = "Adena"
    override val grade = Grade.NO_GRADE
    override val weight = 0
    override val price = 1
    override val isSellable = false
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = true
}
