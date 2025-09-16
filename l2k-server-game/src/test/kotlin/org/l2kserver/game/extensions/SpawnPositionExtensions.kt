package org.l2kserver.game.extensions

import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.SpawnPosition

fun Position.toSpawnPosition(heading: Heading = Heading(0)) = SpawnPosition(
    this.x, this.y, this.z, heading.value.toInt()
)
