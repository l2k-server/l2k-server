package org.l2kserver.game.domain.map

import com.fasterxml.jackson.annotation.JsonRootName
import java.io.File
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.utils.GameDataLoader

/**
 * Class representing town and it's territories
 *
 * @property id Town id
 * @property name Town name
 * @property territories - Geo regions that relate to this town (example: 16_10)
 * @property spawnPositions - If character dies or uses Scroll of escape
 * at this town territory, he will be teleported to one of these positions
 */
@JsonRootName("town")
data class Town(
    val id: Int,
    val name: String,
    val territories: List<String> = emptyList(),
    val spawnPositions: List<Position> = emptyList()
) {

    companion object {

        private val towns = GameDataLoader
            .scanDirectory(File("./data/town"), Town::class.java)
            .associate { it.id to it}

        /**
         * Finds town by [townId]
         *
         * @throws IllegalArgumentException if no town was found
         */
        fun findById(townId: Int) = requireNotNull(towns[townId]) {
            "No town found by id='$townId'"
        }

        /**
         * Finds town, which is closest to the [position]
         */
        fun findClosestByPosition(position: Position): Town {
            val tileX = (position.x shr 0xF) + 20
            val tileY = (position.y shr 0xF) + 18
            return requireNotNull(towns.values.find { it.territories.contains("${tileX}_${tileY}") }) {
                "Territory ${tileX}_${tileY} does not belong to any town!"
            }
        }
    }

}
