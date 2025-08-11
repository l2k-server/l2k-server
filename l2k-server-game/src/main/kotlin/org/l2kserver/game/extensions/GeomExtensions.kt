package org.l2kserver.game.extensions

import java.awt.Shape
import kotlin.random.Random

/**
 * Get random point inside this shape
 */
fun Shape.getRandomPoint(): Pair<Int, Int> {
    val rect = this.bounds

    var x: Int
    var y: Int

    do {
        x = Random.nextInt(rect.x, rect.x + rect.width)
        y = Random.nextInt(rect.y, rect.y + rect.height)
    } while (!this.contains(x.toDouble(), y.toDouble()))

    return Pair(x, y)
}
