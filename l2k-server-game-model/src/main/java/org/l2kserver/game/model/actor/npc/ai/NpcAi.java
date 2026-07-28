package org.l2kserver.game.model.actor.npc.ai;

import org.jetbrains.annotations.NotNull;

/**
 * Artificial Intelligence behavior controller for Non-Player Characters.
 */
public interface NpcAi {

    /**
     * Called every tick
     */
    @NotNull
    Iterable<AiDesire> onTick();
}
