package org.l2kserver.game.utils

object IdUtils {
    private val npcIdIterator = CyclicIdIterator(start = 268304384, end = 268435455)
    private val scatteredItemIdIterator = CyclicIdIterator(start = 268173312, end = 268304383)

    fun getNextScatteredItemId() = scatteredItemIdIterator.next()
    fun getNextNpcId() = npcIdIterator.next()

}
