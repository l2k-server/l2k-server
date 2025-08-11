package org.l2kserver.game.utils

import kotlin.math.abs

class BresenhamIterator(
    startX: Int, startY: Int, endX: Int, endY: Int
): Iterator<Pair<Int, Int>> {

    private var srcX = startX
    private var srcY = startY
    private var dstX = endX
    private var dstY = endY

    private val deltaX = abs(dstX - srcX)
    private val deltaY = abs(dstY - srcY)

    private val stepX = if (dstX > srcX) 1 else -1
    private val stepY = if (dstY > srcY) 1 else -1

    private var error = if (deltaX >= deltaY) deltaX / 2 else deltaY / 2

    override fun hasNext() = srcX != dstX || srcY != dstY

    override fun next(): Pair<Int, Int> {
        if (!hasNext()) throw NoSuchElementException("Iteration has no next element")

        if (deltaX >= deltaY ) {
            if (srcX != dstX) {
                srcX += stepX
                error += deltaY

                if (error >= deltaX) {
                    srcY += stepY
                    error-=deltaX
                }
            }
        }
        else {
            if (srcY != dstY) {
                srcY += stepY
                error += deltaX

                if (error >= deltaY) {
                    srcX += stepX
                    error -= deltaY
                }
            }
        }

        return srcX to srcY
    }

}
