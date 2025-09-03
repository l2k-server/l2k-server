package org.l2kserver.game.data.npc

import org.l2kserver.game.data.ai.GENERAL_AI
import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.reward.RewardItem
import org.l2kserver.game.model.reward.RewardItemGroup
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.zone.Point
import org.l2kserver.game.model.zone.SpawnZone

val GREMLIN = NpcTemplate(
    id = 1018342,
    name = "Gremlin",
    level = 1,
    isAggressive = false,
    isEnemy = true,
    isInvulnerable = false,
    race = NpcRace.FAIRIES,
    collisionBox = CollisionBox(10.0, 15.0),
    stats = Stats(
        maxHp = 62,
        maxMp = 44,
        pAtk = 9,
        pDef = 39,
        accuracy = 33,
        critRate = 44,
        atkSpd = 253,
        mAtk = 3,
        mDef = 32,
        evasion = 33,
        speed = 50,
        castingSpd = 333,
        hpRegen = 3.16,
        mpRegen = 0.91,
        attackRange = 40
    ),
    basicStats = HUMAN_FIGHTER.basicStats,
    reward = Reward(
        exp = 29,
        sp = 2,
        itemGroups = listOf(
            RewardItemGroup(
                chance = 1.0,
                items = listOf(
                    RewardItem(
                        id = 57,
                        name = "Adena",
                        amount = 7..13
                    )
                )
            )
        )
    ),
    spawn = SpawnData(
        respawnDelay = 15_000,
        zones = listOf(SpawnZone(
            name = "Cedric's Training Hall",
            npcAmount = 10,
            zMin = -3109,
            zMax = -3102,
            vertices = listOf(
                Point(-71936, 258355),
                Point(-71289, 257764),
                Point(-70716, 258462),
                Point(-71374, 259023)
            )
        ))
    ),
    ai = GENERAL_AI
)
