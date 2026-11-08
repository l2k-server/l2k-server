package org.l2kserver.game.data.npc.ai

import org.l2kserver.game.model.actor.character.PlayerCharacterInstance
import org.l2kserver.game.model.actor.npc.ai.AiDesire
import org.l2kserver.game.model.actor.npc.ai.AiEvent
import org.l2kserver.game.model.actor.npc.ai.NpcAi

class GreeterAi: NpcAi {

    override fun perceptionRadius() = 400

    override fun onTick(events: List<AiEvent>) = events
        .filterIsInstance<AiEvent.ObjectAppeared>()
        .mapNotNull {
            val player = it.worldObject as? PlayerCharacterInstance
            player?.let { AiDesire.Say("Hello, ${player.name}!") }
        }
}
