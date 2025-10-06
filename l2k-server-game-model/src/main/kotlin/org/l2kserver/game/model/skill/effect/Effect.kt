package org.l2kserver.game.model.skill.effect

@JvmInline
value class SkillEffects private constructor(
    private val effectList: MutableList<Effect>
): Iterable<Effect> by effectList {
    constructor(): this(ArrayList<Effect>())

    fun add(effect: Effect) {
        effectList.add(effect)
    }

}

inline fun effects(builderFunction: SkillEffects.() -> Unit): SkillEffects {
    val skillEffects = SkillEffects()
    skillEffects.builderFunction()

    return skillEffects
}

sealed interface Effect {
    val targetId: Int
}
