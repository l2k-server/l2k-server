package org.l2kserver.game.utils

object IdUtils {
    private val npcIdGenerator = CyclicIdGenerator(start = 268304384, end = 268435455)
    private val scatteredItemIdGenerator = CyclicIdGenerator(start = 268173312, end = 268304383)

    fun getNextScatteredItemId() = scatteredItemIdGenerator.next()
    fun getNextNpcId() = npcIdGenerator.next()

}
