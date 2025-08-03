package org.l2kserver.game.model.actor.position

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Data class that represents object position in the lineage 2 world
 *
 * @property x X coordinate
 * @property y Y coordinate
 * @property z Z coordinate
 *
 */
data class Position(
    val x: Int,
    val y: Int,
    val z: Int
) {

    companion object {
        const val ACCEPTABLE_DELTA = 15
    }

    fun deltaX(other: Position) = other.x - this.x

    fun deltaY(other: Position) = other.y - this.y

    fun deltaZ(other: Position) = other.z - this.z

    /**
     * Calculate distance between the positions (3D)
     */
    fun distanceTo(other: Position): Int {
        val deltaX = deltaX(other).toDouble()
        val deltaY = deltaY(other).toDouble()

        return hypot(hypot(deltaX, deltaY), deltaZ(other).toDouble()).roundToInt()
    }

    fun headingTo(other: Position): Heading {
        val deltaX = deltaX(other).toDouble()
        val deltaY = deltaY(other).toDouble()
        val distanceXY = hypot(deltaX, deltaY)

        val sin = deltaY/ distanceXY
        val cos = deltaX / distanceXY

        return Heading(atan2(-sin, -cos) * 10430.378350470453 + 32768)
    }

    /**
     * Returns true if the distance between positions is lesser than provided
     */
    fun isCloseTo(other: Position, distance: Int = ACCEPTABLE_DELTA) = this.distanceTo(other) <= distance

    /**
     * Calculates position between `this` and [other], which is [distanceToTarget] away from `other`
     *
     * @return If [distanceToTarget] is 0 - copy of [other] position
     * If [distanceToTarget] is greater than real distance between positions - copy of `this` position
     */
    fun positionBetween(other: Position, distanceToTarget: Int): Position {
        if (distanceToTarget == 0) return other.copy()

        val deltaX = this.deltaX(other).toDouble()
        val deltaY = this.deltaY(other).toDouble()
        val distanceXY = hypot(deltaX, deltaY)

        if (distanceXY < distanceToTarget) return this.copy()

        val sin = deltaY / distanceXY
        val cos = deltaX / distanceXY

        val deltaYD = ((distanceXY - distanceToTarget) * sin).roundToInt()
        val deltaXD = ((distanceXY - distanceToTarget) * cos).roundToInt()

        return Position(this.x + deltaXD, this.y + deltaYD, this.z)
    }

}
