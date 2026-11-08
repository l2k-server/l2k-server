package org.l2kserver.game.model.actor.npc.ai;

import org.l2kserver.game.model.actor.GameWorldObject;

/** Event notification for NPC's AI */
public sealed interface AiEvent {

    /**
     * Object has appeared - entered perception zone, was spawned, entered game or was teleported nearby
     * @param worldObject
     */
    record ObjectAppeared(GameWorldObject worldObject) implements AiEvent {}
}
