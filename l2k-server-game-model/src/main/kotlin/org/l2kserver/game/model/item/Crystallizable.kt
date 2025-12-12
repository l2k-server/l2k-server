package org.l2kserver.game.model.item

/**
 * Something, that can be crystallized
 *
 * @property crystalCount How many crystals will be given for this item crystallization
 */
interface Crystallizable {
    val crystalCount: Int
}
