package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.Actor
import java.util.LinkedList

sealed interface SkillEffectEvent
data class DamageEffectEvent(val damage: Int, val target: Actor): SkillEffectEvent

@JvmInline
value class SkillEffectEvents private constructor(
    private val effects: MutableList<SkillEffectEvent>
): Iterable<SkillEffectEvent> by effects {
    constructor(): this(LinkedList<SkillEffectEvent>())

    fun dealDamage(damage: Int, target: Actor) {
        effects.add(DamageEffectEvent(damage, target))
    }

}

sealed interface SkillEffect

interface SingleTargetSkillEffect: SkillEffect {
    fun apply(by: Actor, to: Actor, effectLevel: Int): SkillEffectEvents
}

inline fun effects(builderFunction: SkillEffectEvents.() -> Unit): SkillEffectEvents {
    val skillEffects = SkillEffectEvents()
    skillEffects.builderFunction()

    return skillEffects
}
