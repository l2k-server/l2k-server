package org.l2kserver.game.model.actor.npc.ai

import org.l2kserver.game.model.actor.npc.Npc

class Ai {
    var onIdleAction: (AiIntents.(it: Npc) -> Unit)? = null; private set

    @Suppress("unused")
    fun onIdle(block: AiIntents.(it: Npc) -> Unit): Ai {
        onIdleAction = block
        return this
    }
}
