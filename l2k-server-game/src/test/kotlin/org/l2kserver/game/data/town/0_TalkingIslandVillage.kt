@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.town

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.map.Town

data object TaklingIslandVillage: Town {
    override val id = 0
    override val name = "Talking Island Village"
    override val territories = listOf("16_24", "16_25", "17_24", "17_25", "18_25")

    override val spawnPositions = listOf(
        Position(x = -83990, y = 243336, z = -3728),
        Position(x = -84512, y = 242679, z = -3728),
        Position(x = -84623, y = 243193, z = -3728),
        Position(x = -83742, y = 242214, z = -3728),
        Position(x = -83537, y = 242537, z = -3728),
        Position(x = -83646, y = 243397, z = -3728),
        Position(x = -83808, y = 243637, z = -3728)
    )
}
