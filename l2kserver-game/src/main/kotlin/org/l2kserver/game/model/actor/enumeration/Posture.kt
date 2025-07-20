package org.l2kserver.game.model.actor.enumeration

/**
 * Player character posture - sitting, standing or lying(fake death)
 */
enum class Posture {
    /**
     * Player is sitting
     */
    SITTING,

    /**
     * Player is standing
     */
    STANDING,

    /**
     * Player is lying (fake death)
     */
    LYING
}
