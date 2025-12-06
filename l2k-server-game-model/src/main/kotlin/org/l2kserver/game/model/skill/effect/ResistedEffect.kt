package org.l2kserver.game.model.skill.effect

data class ResistedEffect(
    override val targetId: Int,
    val skillId: Int
): Effect
