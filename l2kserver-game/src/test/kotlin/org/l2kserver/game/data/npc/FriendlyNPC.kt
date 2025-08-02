package org.l2kserver.game.data.npc

import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.L2kNpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.stats.Stats

val GRAND_MASTER_ROIEN = L2kNpcTemplate(
    id = 1_030_008,
    name = "Roien",
    title = "Grand Master",
    level = 70,
    isAggressive = false,
    isEnemy = false,
    isInvulnerable = true,
    race = NpcRace.HUMANS,
    collisionBox = CollisionBox(8.0, 23.5),
    stats = Stats(
        maxHp = Int.MAX_VALUE,
        maxMp = Int.MAX_VALUE,
        pDef = Int.MAX_VALUE,
        mDef = Int.MAX_VALUE
    ),
    spawn = SpawnData(
        positions = listOf(SpawnPosition(-71384, 258305, -3109, 42000))
    ),
    replicas = listOf("""
        <html>
            <body>
                Grand Master Roien:<br>
                Welcome. I am Grand Master Roien, of Cedric's Training Hall.<br>
                This school was established by the renowned Paladin Sir Cedric,
                loyal subject of King Raoul the Unifier, to train young Fighters.
                One day, perhaps, in your travels you will be able to meet Sir Cedric,
                whom I have the honor to call my uncle, in the Kingdom of Aden.<br>
                <a action=\"bypass -h npc_%objectId%_Quest\">Quest</a>
            </body>
        </html>
    """.trimIndent())
)
