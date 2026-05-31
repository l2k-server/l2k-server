package org.l2kserver.game.data.item.book

import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.item.Book
import org.l2kserver.game.model.item.Grade

data object TutorialGuide: Book() {
    override val id = 5588
    override val name = "Tutorial Guide"
    override val grade = Grade.NO_GRADE
    override val weight = 10
    override val price = 0
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false

    override val text = HtmlRegistry.findById("tutobook.htm")
}
