package org.l2kserver.game.data.ai

import org.l2kserver.game.model.actor.npc.ai.Ai
import org.l2kserver.game.model.actor.position.Position
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val WANDERING_DISTANCE = 150.0

val GENERAL_AI = Ai()
    .onIdle {
        if (Random.nextInt(0, 1000) < 10) {
            //Move to random point at the WANDERING_DISTANCE distance
            val degree = Math.toRadians(Random.nextDouble(0.0, 360.0))
            val sin = sin(degree)
            val cos = cos(degree)

            val targetPosition = Position (
                x = it.position.x + (WANDERING_DISTANCE * cos).roundToInt(),
                y = it.position.y + (WANDERING_DISTANCE * sin).roundToInt(),
                z = it.position.z
            )

            moveTo(targetPosition)
        }
    }
