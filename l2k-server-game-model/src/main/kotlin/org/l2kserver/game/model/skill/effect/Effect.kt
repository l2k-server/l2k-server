package org.l2kserver.game.model.skill.effect

sealed interface Effect {
    val targetId: Int
}

inline fun effects(action: MutableList<Effect>.() -> Unit) = mutableListOf<Effect>().apply { action() }
