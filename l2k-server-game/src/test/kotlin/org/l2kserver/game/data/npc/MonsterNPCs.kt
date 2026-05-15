package org.l2kserver.game.data.npc

import org.l2kserver.game.data.characterclass.HumanFighter
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.Npc
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.actor.npc.ai.aiIntents
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.reward.Reward
import org.l2kserver.game.model.reward.RewardItem
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.zone.Point
import org.l2kserver.game.model.zone.SpawnZone
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data object Gremlin: Npc {
    private const val WANDERING_DISTANCE = 75
    private const val MAX_DISTANCE_FROM_SPAWN = 150

    override val id = 1018342
    override val name = "Gremlin"
    override val level = 1
    override val race = NpcRace.FAIRIES
    override val collisionBox = CollisionBox(10.0, 15.0)
    override val stats = CombatStats(
        maxHp = 62.0,
        maxMp = 44.0,
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
    )
    override val basicStats = HumanFighter.basicStats
    override val reward = Reward(
        exp = 29,
        sp = 2,
        itemGroups = listOf(
            1.0 to listOf(
                RewardItem(
                    id = 57,
                    name = "Adena",
                    amount = 7..13
                )
            )
        )
    )
    override val spawn = SpawnData(
        respawnDelay = 15_000,
        zones = listOf(
            SpawnZone(
                name = "Cedric's Training Hall",
                npcAmount = 15,
                zMin = -3109,
                zMax = -3102,
                vertices = listOf(
                    Point(-71936, 258355),
                    Point(-71289, 257764),
                    Point(-70716, 258462),
                    Point(-71374, 259023)
                )
            ),
            SpawnZone(
                name = "Einhovant's School of Magic",
                npcAmount = 15,
                zMin = -3568,
                zMax = -3568,
                vertices = listOf(
                    Point(-90395, 248656),
                    Point(-90207, 248354),
                    Point(-90810, 247945),
                    Point(-91021, 248237)
                )
            )
        )
        //Uncomment this to enable GREMLINOCALYPSE at talking island
//        zones = listOf(SpawnZone(
//            name = "Talking Island",
//            npcAmount = 250_000,
//            zMin = -3748,
//            zMax = -3032,
//            vertices = listOf(
//                Point(Position.MAP_MIN_X, Position.MAP_MAX_Y),
//                Point(Position.MAP_MIN_X, Position.MAP_MAX_Y - Position.GEO_TILE_SIZE * 2),
//                Point(Position.MAP_MIN_X + Position.GEO_TILE_SIZE, Position.MAP_MAX_Y - Position.GEO_TILE_SIZE * 2),
//                Point(Position.MAP_MIN_X + Position.GEO_TILE_SIZE, Position.MAP_MAX_Y - Position.GEO_TILE_SIZE),
//                Point(Position.MAP_MIN_X + Position.GEO_TILE_SIZE * 2, Position.MAP_MAX_Y - Position.GEO_TILE_SIZE),
//                Point(Position.MAP_MIN_X + Position.GEO_TILE_SIZE * 2, Position.MAP_MAX_Y)
//            )
//        ))
    )

    override fun isEnemyOf(other: ActorInstance) = true

    override fun onIdle(npc: NpcInstance) = aiIntents {
//        if (Random.nextInt(100) < 5) {
//            //Move to random point at the WANDERING_DISTANCE distance
//            val degree = Math.toRadians(Random.nextDouble(0.0, 360.0))
//            val sin = sin(degree)
//            val cos = cos(degree)
//
//            val targetPosition = Position (
//                x = npc.position.x + (WANDERING_DISTANCE * cos).roundToInt(),
//                y = npc.position.y + (WANDERING_DISTANCE * sin).roundToInt(),
//                z = npc.position.z
//            )
//
//            //Prevent moving too far
//            npc.spawnedAt.spawnPosition?.let {
//                if (!targetPosition.isCloseTo(it.toPositionAndHeading().first, MAX_DISTANCE_FROM_SPAWN))
//                    return@aiIntents
//            }
//
//            npc.spawnedAt.spawnZone?.let {
//                if (!it.contains(targetPosition))
//                    return@aiIntents
//            }
//
//            moveTo(targetPosition)
//        }
    }
}

data object FatDummyGremlin: Npc {
    override val id = 1018342
    override val name = "Fat Dummy Gremlin"
    override val level = 1
    override val race = NpcRace.FAIRIES
    override val collisionBox = CollisionBox(10.0, 15.0)
    override val stats = CombatStats(
        maxHp = 10_000.0,
        maxMp = 44.0,
        pAtk = 9,
        pDef = 39,
        accuracy = 33,
        critRate = 44,
        atkSpd = 253,
        mAtk = 3,
        mDef = 32,
        evasion = 0,
        speed = 50,
        castingSpd = 333,
        hpRegen = 3.16,
        mpRegen = 0.91,
        attackRange = 40
    )
    override val basicStats = HumanFighter.basicStats
    override fun isEnemyOf(other: ActorInstance) = true
}
