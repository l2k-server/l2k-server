package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.npc.NpcState

/**
 * Changes NPC state to 'Fighting' state.
 * If `this` is already a 'Fighting' state, adds [opponent] to [NpcState.Fighting.opponents] list
 */
fun NpcState.toFighting(opponent: ActorInstance, menace: Int = 0): NpcState.Fighting = synchronized(this) {
    val newState = (this as? NpcState.Fighting) ?: NpcState.Fighting()
    val currentMenace = newState.opponents[opponent] ?: 0
    newState.opponents[opponent] = currentMenace + menace

    newState
}
