package org.l2kserver.game.model.skill.effect

/**
 * The effect of resurrection somebody
 *
 * @property targetId Target of the effect
 * @property restoredExp Which part of lost exp must be restored. Must be from 0 to 1
 */
data class ResurrectionEffect(override val targetId: Int, val restoredExp: Double = 0.0): Effect {
    init {
        require(restoredExp in 0.0..1.0) {
            "Part of restored EXP cannot be lesser that 0, and greater than 1"
        }
    }
}
