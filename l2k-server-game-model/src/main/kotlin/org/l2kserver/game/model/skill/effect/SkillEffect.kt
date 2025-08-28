package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.actor.Actor
import org.l2kserver.game.model.skill.effect.event.DamageEvent
import org.l2kserver.game.model.skill.effect.event.EffectEvent
import java.util.LinkedList

@JvmInline
value class SkillEffectEvents private constructor(
    private val effects: MutableList<EffectEvent>
): Iterable<EffectEvent> by effects {
    constructor(): this(LinkedList<EffectEvent>())

    /** Applies the event of dealing damage to [target] */
    fun dealDamage(
        damage: Int,
        target: Actor,
        isCritical: Boolean = false,
        isBlocked: Boolean = false,
        overhitPossible: Boolean = false
    ) {
        effects.add(DamageEvent(
            target.id, damage, isCritical = isCritical, isBlocked = isBlocked, overhitPossible = overhitPossible
        ))
    }

    /** Applies the event of [target]'s evasion */
    fun miss(target: Actor) {
        effects.add(DamageEvent(target.id, 0, isAvoided = true))
    }

}

sealed interface SkillEffect

interface SingleTargetSkillEffect: SkillEffect {
    fun apply(caster: Actor, target: Actor, effectLevel: Int): SkillEffectEvents
}

inline fun effects(builderFunction: SkillEffectEvents.() -> Unit): SkillEffectEvents {
    val skillEffects = SkillEffectEvents()
    skillEffects.builderFunction()

    return skillEffects
}
