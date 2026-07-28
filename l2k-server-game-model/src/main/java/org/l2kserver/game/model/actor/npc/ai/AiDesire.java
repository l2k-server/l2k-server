package org.l2kserver.game.model.actor.npc.ai;

import org.l2kserver.game.model.actor.ActorInstance;
import org.l2kserver.game.model.actor.position.Position;

/**
 * Some action, that NPC wants to do.
 */
public sealed interface AiDesire {

    /**
     * Intention to say something.
     * By default, it has the greatest priority cause the action is
     * performed instantly and does not interrupt any other actions
     */
    record Say(String message) implements AiDesire {}

    /**
     * Intention to move somewhere.
     * By default, it has the lowest priority - this action is performed usually when NPC is idle
     */
    record Move(Position position) implements AiDesire {}

    /**
     * Intention to attack target.
     */
    record Attack(ActorInstance target) implements AiDesire {

        /**
         * Calculates the desire to attack actor, who dealt `damageGot` damage to this NPC
         *
         * @param damageGot Damage got by the NPC
         * @param myMaxHp MaxHP stat of the NPC
         */
        public static int damageGotToDesire(int damageGot, int myMaxHp) {
            return (int)((double) damageGot / myMaxHp / 0.05 * 100);
        }
    }
}