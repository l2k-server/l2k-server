package org.l2kserver.game.model.actor.character

/**
 * Player's PvP state
 *
 * @property NOT_IN_PVP Player is not in PvP
 * @property PVP Player is in PvP
 * @property PVP_ENDING Player's PvP state is going to end (blinking violet name)
 */
enum class PvpState {
    NOT_IN_PVP,
    PVP,
    PVP_ENDING
}
