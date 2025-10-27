package org.l2kserver.game.model.skill.effect

@JvmInline
value class Effects private constructor(
    private val effectList: MutableList<Effect>
): Iterable<Effect> by effectList {
    constructor(): this(ArrayList<Effect>())

    fun add(effect: Effect) {
        effectList.add(effect)
    }

}

sealed interface Effect

inline fun effects(builderFunction: Effects.() -> Unit) = Effects().apply { builderFunction() }
