package org.l2kserver.game.data.npc

import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.stats.CombatStats

val GRAND_MASTER_ROIEN = NpcTemplate(
    id = 1_030_008,
    name = "Roien",
    title = "Grand Master",
    level = 70,
    isAggressive = false,
    isEnemy = false,
    isInvulnerable = true,
    race = NpcRace.HUMANS,
    collisionBox = CollisionBox(8.0, 23.5),
    stats = CombatStats(
        maxHp = Int.MAX_VALUE,
        maxMp = Int.MAX_VALUE,
        pDef = Int.MAX_VALUE,
        mDef = Int.MAX_VALUE
    ),
    basicStats = HUMAN_FIGHTER.basicStats,
    spawn = SpawnData(
        positions = listOf(SpawnPosition(-71384, 258305, -3109, 42000))
    ),
    replicas = listOf(
        """
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
        """.replace("\\s+".toRegex(), " ")
    )
)

val GRAND_MAGISTER_GALLINT = NpcTemplate(
    id = 1_030_017,
    name = "Gallint",
    title = "Grand Magister",
    level = 70,
    isAggressive = false,
    isEnemy = false,
    isInvulnerable = true,
    race = NpcRace.HUMANS,
    collisionBox = CollisionBox(8.0, 24.0),
    stats = CombatStats(
        maxHp = Int.MAX_VALUE,
        maxMp = Int.MAX_VALUE,
        pDef = Int.MAX_VALUE,
        mDef = Int.MAX_VALUE
    ),
    basicStats = HUMAN_FIGHTER.basicStats,
    spawn = SpawnData(
        positions = listOf(SpawnPosition(-91008, 248016, -3568, 6000))
    ),
    replicas = listOf(
        """
        <html>
            <body>
                Grand Sorcerer Gallint:<br>
                Welcome. I am Grand Sorcerer Gallint of Einhovant's School of Wizardry. 
                This is a school for Mystics, established by the famed alchemist Einhovant.<br>
                I was once an apprentice of his but he vanished soon after establishing this school, 
                and I have not heard from him since. I wonder if he still lives...<br>
                <a action="bypass -h npc_%objectId%_Quest">Quest</a>
            </body>
        </html>
        """.replace("\\s+".toRegex(), " ")
    )
)
