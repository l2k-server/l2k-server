package org.l2kserver.game.data.ai

import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.actor.npc.ai.Ai
import org.l2kserver.game.model.actor.npc.ai.aiIntents
import org.l2kserver.game.model.actor.position.Position
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val WANDERING_DISTANCE = 75
private const val MAX_DISTANCE_FROM_SPAWN = 150

object GeneralAi: Ai {

    override fun onIdle(npc: NpcInstance) = aiIntents {
        if (Random.nextInt(100) < 5) {
            //Move to random point at the WANDERING_DISTANCE distance
            val degree = Math.toRadians(Random.nextDouble(0.0, 360.0))
            val sin = sin(degree)
            val cos = cos(degree)

            val targetPosition = Position (
                x = npc.position.x + (WANDERING_DISTANCE * cos).roundToInt(),
                y = npc.position.y + (WANDERING_DISTANCE * sin).roundToInt(),
                z = npc.position.z
            )

            //Prevent moving too far
            npc.spawnedAt.spawnPosition?.let {
                if (!targetPosition.isCloseTo(it.toPositionAndHeading().first, MAX_DISTANCE_FROM_SPAWN))
                    return@aiIntents
            }

            npc.spawnedAt.spawnZone?.let {
                if (!it.contains(targetPosition))
                    return@aiIntents
            }

            moveTo(targetPosition)
        }
    }

}
