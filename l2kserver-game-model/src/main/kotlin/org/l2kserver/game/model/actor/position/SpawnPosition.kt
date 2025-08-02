package org.l2kserver.game.model.actor.position

/**
 * Represents spawn position of game object - x,y,z coordinates and heading
 * @see Position
 * @see Heading
 */
data class SpawnPosition(
    val x: Int,
    val y: Int,
    val z: Int,
    val heading: Int
)

fun SpawnPosition.toPositionAndHeading() = Position(this.x, this.y, this.z) to Heading(this.heading)

fun Position.toSpawnPosition(heading: Heading = Heading(0)) = SpawnPosition(
    this.x, this.y, this.z, heading.value.toInt()
) //TODO Перенести в TEST
