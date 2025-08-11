package org.l2kserver.game.model.extensions

import java.awt.geom.Path2D
import org.l2kserver.game.model.zone.Point

fun List<Point>.toPath2D(): Path2D {
    val shape = Path2D.Double()
    shape.moveTo(this.first().x.toDouble(), this.first().y.toDouble())
    for (i: Int in 1..< this.size) {
        shape.lineTo(this[i].x.toDouble(), this[i].y.toDouble())
    }
    shape.closePath()

    return shape
}
