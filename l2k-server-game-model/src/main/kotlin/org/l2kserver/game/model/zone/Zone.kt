package org.l2kserver.game.model.zone

import java.awt.Shape
import java.awt.geom.Point2D
import org.l2kserver.game.model.actor.position.Position

data class Point(val x: Int, val y: Int)

abstract class Zone(
    val zMin: Int,
    val zMax: Int,
    val shape: Shape
) {
    init {
        require(zMin <= zMax) { "Minimal Z coordinate of zone cannot be greater than maximum" }
    }

    fun contains(position: Position): Boolean {
        val point = Point2D.Double(position.x.toDouble(), position.y.toDouble())
        return shape.contains(point) && zMin <= position.z && zMax >= position.z
    }

}
