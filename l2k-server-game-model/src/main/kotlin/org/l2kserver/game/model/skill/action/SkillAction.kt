package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.action.effect.DamageEffect
import org.l2kserver.game.model.skill.action.effect.Effect

@JvmInline
value class SkillEffects private constructor(
    private val effects: MutableList<Effect>
): Iterable<Effect> by effects {
    constructor(): this(ArrayList<Effect>())

    /** Applies the event of dealing damage to [target] */
    fun hit(
        damage: Int,
        target: ActorInstance,
        isCritical: Boolean = false,
        isBlocked: Boolean = false,
        overhitPossible: Boolean = false
    ) {
        effects.add(DamageEffect(
            target.id, damage, isCritical = isCritical, isBlocked = isBlocked, overhitPossible = overhitPossible
        ))
    }

    /** Applies the event of [target]'s evasion */
    fun miss(target: ActorInstance) {
        effects.add(DamageEffect(target.id, 0, isAvoided = true))
    }

}

sealed interface SkillAction

interface SingleTargetSkillAction: SkillAction {
    fun applyTo(target: ActorInstance, caster: ActorInstance, effectLevel: Int): SkillEffects
}

inline fun effects(builderFunction: SkillEffects.() -> Unit): SkillEffects {
    val skillEffects = SkillEffects()
    skillEffects.builderFunction()

    return skillEffects
}
