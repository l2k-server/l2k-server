package org.l2kserver.game.model.actor.npc.ai;

import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Artificial Intelligence behavior controller for Non-Player Characters.
 */
public interface NpcAi {

    /**
     * The radius within which npc notices new [GameWorldObject]s
     * Default: 0
     */
    default int perceptionRadius() {
        return 0;
    }

    /**
     * Called every tick
     * @param events Events that happened since previous call
     */
    @NotNull
    Iterable<AiDesire> onTick(@NotNull List<AiEvent> events);
}
