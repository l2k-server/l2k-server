package org.l2kserver.game.model.actor.npc.ai

import org.l2kserver.game.model.actor.npc.NpcInstance

class Ai {
    var onIdleAction: (AiIntents.(it: NpcInstance) -> Unit)? = null; private set

    @Suppress("unused")
    fun onIdle(block: AiIntents.(it: NpcInstance) -> Unit): Ai {
        onIdleAction = block
        return this
    }
}
