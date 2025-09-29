package org.l2kserver.game.data.item.book

import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.item.template.BookTemplate
import org.l2kserver.game.model.item.template.Grade

val TUTORIAL_GUIDE = BookTemplate(
    id = 5588,
    name = "Tutorial Guide",
    grade = Grade.NO_GRADE,
    weight = 10,
    price = 0,
    isSellable = false,
    isDroppable = false,
    isDestroyable = true,
    isExchangeable = false,
    text = HtmlRegistry.findById("tutobook.htm")
)
