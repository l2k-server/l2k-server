package org.l2kserver.game.utils

import java.util.concurrent.atomic.AtomicInteger

/**
 * Iterator, that generates ID in given diapason. If new id value is greater than end, it starts from the beginning
 */
class CyclicIdIterator(
    private val start: Int = Int.MIN_VALUE,
    private val end: Int = Int.MAX_VALUE
): Iterator<Int> {

    private var current = AtomicInteger(start)
    override fun hasNext() = true

    @Synchronized
    override fun next(): Int {
        if (current.get() > end) current.set(start)
        return current.getAndIncrement()
    }

}
