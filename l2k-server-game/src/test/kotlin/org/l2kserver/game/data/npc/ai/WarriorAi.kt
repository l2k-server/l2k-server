package org.l2kserver.game.data.npc.ai

import org.l2kserver.game.model.actor.npc.NpcInstance
import org.l2kserver.game.model.actor.npc.ai.AiDesire
import org.l2kserver.game.model.actor.npc.ai.AiEvent
import org.l2kserver.game.model.actor.npc.ai.NpcAi
import org.l2kserver.game.model.actor.position.Position
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Simple AI, that makes NPC wandering around if it is idle, or attack actor, who attacked him
 */
class WarriorAi(private val self: NpcInstance, private val wanderingDistance: Int): NpcAi {

    override fun onTick(events: MutableList<AiEvent>): MutableList<AiDesire> {
        val desires = ArrayList<AiDesire>()

        // 1% chance to start wandering
        if (ThreadLocalRandom.current().nextInt(1000) < 5) {
            val spawnPosition = self.spawnedAt.spawnPosition

            val targetPosition = Position(
                Random.nextInt(spawnPosition.x - wanderingDistance, spawnPosition.x + wanderingDistance),
                Random.nextInt(spawnPosition.y - wanderingDistance, spawnPosition.y + wanderingDistance),
                self.position.z
            )

            desires.add(AiDesire.Move(targetPosition))
        }

        return desires
    }
}
