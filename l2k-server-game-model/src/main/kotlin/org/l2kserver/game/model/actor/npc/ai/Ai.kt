package org.l2kserver.game.model.actor.npc.ai

import org.l2kserver.game.model.actor.npc.NpcInstance

interface Ai {

    fun onIdle(npc: NpcInstance): AiIntents {
        return AiIntents()
    }

}

inline fun aiIntents(action: AiIntents.() -> Unit): AiIntents {
    val intents = AiIntents()
    intents.action()

    return intents
}
