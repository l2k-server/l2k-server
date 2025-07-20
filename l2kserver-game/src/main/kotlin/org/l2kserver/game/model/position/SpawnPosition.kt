package org.l2kserver.game.model.position

import com.fasterxml.jackson.annotation.JsonCreator

data class SpawnPosition(
    val position: Position,
    val heading: Heading
) {
    @JsonCreator
    constructor(x: Int, y: Int, z: Int, heading: Int): this(position = Position(x, y, z), heading = Heading(heading))
}
