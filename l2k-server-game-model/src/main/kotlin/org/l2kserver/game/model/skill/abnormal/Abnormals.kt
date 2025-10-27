package org.l2kserver.game.model.skill.abnormal

@JvmInline
value class Abnormals private constructor(
    private val effectList: MutableList<Abnormal>
): Iterable<Abnormal> by effectList {
    constructor(): this(ArrayList<Abnormal>())

    fun add(effect: Abnormal) {
        effectList.add(effect)
    }

}

sealed interface Abnormal

inline fun abnormals(builderFunction: Abnormals.() -> Unit) = Abnormals().apply { builderFunction() }
