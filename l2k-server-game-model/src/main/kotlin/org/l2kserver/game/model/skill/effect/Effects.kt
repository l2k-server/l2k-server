package org.l2kserver.game.model.skill.effect

@JvmInline
value class Effects private constructor(
    private val effectList: MutableList<Effect> = ArrayList()
): Iterable<Effect> by effectList {

    constructor(vararg effects: Effect): this() {
        effectList.addAll(effects)
    }

    fun add(effect: Effect) = effectList.add(effect)
}

sealed interface Effect {
    val targetId: Int
}

inline fun effects(builderFunction: Effects.() -> Unit) = Effects().apply { builderFunction() }
