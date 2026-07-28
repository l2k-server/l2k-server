package org.l2kserver.game.model.actor.npc;

import org.jetbrains.annotations.NotNull;
import org.l2kserver.game.model.actor.ActorInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Current state of NPC */
public sealed interface NpcState {

    /**
     * Npc is currently doing nothing
     */
    record Idle() implements NpcState {}

    /** Npc is fighting with [opponents] */
    final class Fighting implements NpcState {
        private final Map<ActorInstance, Integer> opponents = new ConcurrentHashMap<>();

        /** Actors, who fights with NPC. Key - actor, value - his aggro points */
        @NotNull
        public Map<ActorInstance, Integer> getOpponents() {
            return opponents;
        }

        @Override
        public String toString() {
            return "Fighting{opponents=" + opponents + '}';
        }
    }
}
