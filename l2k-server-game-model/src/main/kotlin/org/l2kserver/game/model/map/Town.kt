package org.l2kserver.game.model.map

import org.l2kserver.game.model.GameData
import org.l2kserver.game.model.GameDataRegistry
import org.l2kserver.game.model.actor.position.Position

/** Stores all the towns data */
object TownRegistry: GameDataRegistry<Town>() {

    /** Identifier of town, where cruel criminals should be respawned */
    var teleportCriminalsTo: Int = 17

    /** Default respawn position. By default, (¯\_(ツ)_/¯) central square of game world capital */
    var defaultSpawnPosition = Position(147451, 27014, -2205)

    /**
     * If [isOutlaw] is true, returns random spawn point in [teleportCriminalsTo] town, otherwise
     * returns spawn point in the town, closest to provided [position],
     * or [defaultSpawnPosition] if no town was found by the territory or [teleportCriminalsTo] id
     */
    fun getRandomSpawnPointByPosition(position: Position, isOutlaw: Boolean = false) =
        (if (isOutlaw) findByIdOrNull(teleportCriminalsTo)
        else findClosestByPosition(position))
            ?.spawnPositions?.random() ?: defaultSpawnPosition

    /** Finds town, which is closest to the [position] */
    fun findClosestByPosition(position: Position) = this.find {
        it.territories.contains("${position.getTileX()}_${position.getTileY()}")
    }

}

/**
 * Represents town and it's territories
 *
 * @property id Town id
 * @property name Town name
 * @property territories - Geo regions that relate to this town (example: 16_10)
 * @property spawnPositions - If character dies or uses Scroll of escape
 * at this town territory, he will be teleported to one of these positions
 */
interface Town: GameData {
    override val id: Int
    val name: String
    val territories: List<String>
    val spawnPositions: List<Position>
}
