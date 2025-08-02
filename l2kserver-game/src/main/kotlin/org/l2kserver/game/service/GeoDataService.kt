package org.l2kserver.game.service

import com.l2jserver.geodriver.Cell
import com.l2jserver.geodriver.GeoDriver
import org.l2kserver.game.extensions.getRandomPoint
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.zone.Zone
import org.l2kserver.game.utils.BresenhamIterator
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class GeoDataService(
    private val geoDriver: GeoDriver
) {

    fun getRandomSpawnPosition(collisionBox: CollisionBox, zone: Zone): Position {
        while (true) {
            val (x, y) = zone.shape.getRandomPoint()
            val z = if (zone.zMin == zone.zMax) zone.zMin else Random.nextInt(zone.zMin, zone.zMax)

            val collisionRect = arrayOf(
                (x - collisionBox.radius).toInt() to y,
                (x + collisionBox.radius).toInt() to y,
                x to (y - collisionBox.radius).toInt(),
                x to (y + collisionBox.radius).toInt()
            )

            if (collisionRect.all { (zone.zMin..zone.zMax).contains(getNearestZ(it.first, it.second, z)) })
                return Position(
                    x = x,
                    y = y,
                    z = getNearestZ(x, y, z)
                )
        }
    }

    fun getNearestZ(x: Int, y: Int, z: Int) = geoDriver.getNearestZ(
        geoDriver.getGeoX(x),
        geoDriver.getGeoY(y),
        z
    )

    /**
     * Returns the closest position to the target that the character can move to
     * (If there is an obstacle between position, returns the point just before obstacle)
     */
    fun getAvailableTargetPosition(startPosition: Position, targetPosition: Position): Position {
        val geoXStart = geoDriver.getGeoX(startPosition.x)
        val geoYStart = geoDriver.getGeoY(startPosition.y)
        val startZ = geoDriver.getNearestZ(geoXStart, geoYStart, startPosition.z)

        val geoXTarget = geoDriver.getGeoX(targetPosition.x)
        val geoYTarget = geoDriver.getGeoY(targetPosition.y)

        val iterator = BresenhamIterator(geoXStart, geoYStart, geoXTarget, geoYTarget)
        var lastPoint = Triple(geoXStart, geoYStart, startZ)

        for ((x, y) in iterator) {
            val z = geoDriver.getNearestZ(x, y, startPosition.z)
            if (geoDriver.hasGeoPos(lastPoint.first, lastPoint.second)) {
                val nswe = getNSWE(lastPoint.first, lastPoint.second, x, y)
                if (!checkAntiCornerCut(lastPoint.first, lastPoint.second, lastPoint.third, nswe)) break
            }

//            if (z - lastPoint.third > MAX_MOVABLE_DELTA_Z) break

            lastPoint = Triple(x, y, z)
        }

        return Position(
            x = geoDriver.getWorldX(lastPoint.first),
            y = geoDriver.getWorldY(lastPoint.second),
            z = lastPoint.third
        )
    }

    /**
     * @return cardinal direction of moving (North,South,West,East etc.)
     */
    private fun getNSWE(prevX: Int, prevY: Int, x: Int, y: Int): Int {
        return (if (x > prevX) {
            if (y > prevY) Cell.NSWE_SOUTH_EAST
            else if (y < prevY) Cell.NSWE_NORTH_EAST
            else Cell.NSWE_EAST
        } else if (x < prevX) {
            if (y > prevY) Cell.NSWE_SOUTH_WEST
            else if (y < prevY) Cell.NSWE_NORTH_WEST
            else Cell.NSWE_WEST
        } else {
            if (y > prevY) Cell.NSWE_NORTH
            else if (y < prevY) Cell.NSWE_SOUTH
            else error("Failed to compute NSWE")
        }).toInt()
    }

    private fun checkAntiCornerCut(geoX: Int, geoY: Int, worldZ: Int, nswe: Int): Boolean {
        var checkResult = true

        when (nswe) {
            Cell.NSWE_NORTH_EAST.toInt() ->
                checkResult = geoDriver.checkNearestNswe(
                    geoX,
                    geoY - 1,
                    worldZ,
                    Cell.NSWE_EAST.toInt()
                ) && geoDriver.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH.toInt())

            Cell.NSWE_NORTH_WEST.toInt() ->
                checkResult = geoDriver.checkNearestNswe(
                    geoX,
                    geoY - 1,
                    worldZ,
                    Cell.NSWE_WEST.toInt()
                ) && geoDriver.checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_NORTH.toInt())

            Cell.NSWE_SOUTH_EAST.toInt() ->
                checkResult = geoDriver.checkNearestNswe(
                    geoX,
                    geoY + 1,
                    worldZ,
                    Cell.NSWE_EAST.toInt()
                ) && geoDriver.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH.toInt())

            Cell.NSWE_SOUTH_WEST.toInt() ->
                checkResult = geoDriver.checkNearestNswe(
                    geoX,
                    geoY + 1,
                    worldZ,
                    Cell.NSWE_WEST.toInt()
                ) && geoDriver.checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH.toInt())
        }

        return checkResult && geoDriver.checkNearestNswe(geoX, geoY, worldZ, nswe)
    }

}
