package org.l2kserver.game.data.npc

import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.html.HtmlRegistry
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
    replica = HtmlRegistry.findById("roien001.htm")
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
    replica = HtmlRegistry.findById("gallint001.htm")
)
