package org.l2kserver.game.utils

/**
 * Generates ID in given diapason. If new id value is greater than end, it starts from the beginning
 */
class CyclicIdGenerator(
    private val start: Int = Int.MIN_VALUE,
    private val end: Int = Int.MAX_VALUE
) {
    private var current = start

    @Synchronized
    fun next(): Int {
        if (current > end) current = start
        return current++
    }

}
