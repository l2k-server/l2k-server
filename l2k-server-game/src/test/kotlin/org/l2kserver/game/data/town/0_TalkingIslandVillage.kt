@file:Suppress("MatchingDeclarationName")
package org.l2kserver.game.data.town

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.map.Town

data object TaklingIslandVillage: Town {
    override val id = 0
    override val name = "Talking Island Village"
    override val territories = listOf("16_24", "16_25", "17_24", "17_25", "18_25")

    override val spawnPositions = listOf(
        Position(-83990, 243336, -3728),
        Position(-84512, 242679, -3728),
        Position(-84623, 243193, -3728),
        Position(-83742, 242214, -3728),
        Position(-83537, 242537, -3728),
        Position(-83646, 243397, -3728),
        Position(-83808, 243637, -3728)
    )
}
