package org.l2kserver.game.model.actor.character;

/**
 * Player's PvP state
 */
public enum PvpState {
    /** Player is not in PvP */
    NOT_IN_PVP,

    /** Player is in PvP */
    PVP,

    /** Player's PvP state is going to end (blinking violet name) */
    PVP_ENDING
}
