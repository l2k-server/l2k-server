package org.l2kserver.game.utils

object IdUtils {
    private val npcIdIterator = CyclicIdIterator(268304384, 268435455)
    private val scatteredItemIdIterator = CyclicIdIterator(268173312, 268304383)

    fun getNextScatteredItemId() = scatteredItemIdIterator.next()
    fun getNextNpcId() = npcIdIterator.next()

}
