package org.l2kserver.game.model.item

import org.l2kserver.game.model.item.template.Grade

/**
 * Something, that can be enchanted
 *
 * @property grade Grade of enchant scrolls, to enchant this item
 * @property enchantLevel This item enchant level
 */
interface Enchantable {
    val grade: Grade
    var enchantLevel: Int
}
