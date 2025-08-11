package org.l2kserver.game.model.zone

import org.l2kserver.game.model.extensions.toPath2D

class SpawnZone(
    val name: String,
    val npcAmount: Int = 1,
    zMin: Int,
    zMax: Int,
    private val vertices: List<Point>
): Zone(zMin, zMax, vertices.toPath2D()) {

    override fun toString(): String {
        return "SpawnZone(name='$name', amount=$npcAmount, vertices=$vertices)"
    }

}
