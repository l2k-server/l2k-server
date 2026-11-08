package org.l2kserver.game.model.actor.npc.ai;

import org.l2kserver.game.model.actor.ActorInstance;
import org.l2kserver.game.model.actor.position.Position;

/**
 * Some action, that NPC wants to do.
 */
public sealed interface AiDesire {

    /**
     * Intention to say something.
     */
    record Say(String message) implements AiDesire {}

    /**
     * Intention to move somewhere.
     */
    record Move(Position position) implements AiDesire {}

    /**
     * Intention to attack target.
     */
    record Attack(ActorInstance target) implements AiDesire {}
}