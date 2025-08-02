package org.l2kserver.game.model.actor.npc.ai

import org.l2kserver.game.model.actor.npc.L2kNpc

class Ai {
    var onIdleAction: (AiIntents.(it: L2kNpc) -> Unit)? = null; private set

    @Suppress("unused")
    fun onIdle(block: AiIntents.(it: L2kNpc) -> Unit): Ai {
        onIdleAction = block
        return this
    }
}
