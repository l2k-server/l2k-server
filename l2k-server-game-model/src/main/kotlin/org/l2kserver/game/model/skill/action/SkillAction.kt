package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.action.effect.DamageEffect
import org.l2kserver.game.model.skill.action.effect.Effect

@JvmInline
value class SkillEffects private constructor(
    private val effectList: MutableList<Effect>
): Iterable<Effect> by effectList {
    constructor(): this(ArrayList<Effect>())

    /** Applies the event of dealing damage to [target] */
    fun hit(
        damage: Int,
        target: ActorInstance,
        usedSs: Boolean = false,
        isCritical: Boolean = false,
        isBlocked: Boolean = false,
        overhitPossible: Boolean = false
    ) {
        effectList.add(DamageEffect(
            target.id,
            damage,
            usedSoulshot = usedSs,
            isCritical = isCritical,
            isBlocked = isBlocked,
            overhitPossible = overhitPossible
        ))
    }

    /** Applies the event of [target]'s evasion */
    fun miss(target: ActorInstance) {
        effectList.add(DamageEffect(target.id, 0, isAvoided = true))
    }

}

interface SkillAction

interface SingleTargetPhysicalSkillAction: SkillAction {
    fun applyTo(target: ActorInstance, caster: ActorInstance, effectLevel: Int, usedSoulshot: Boolean): SkillEffects
}

inline fun effects(builderFunction: SkillEffects.() -> Unit): SkillEffects {
    val skillEffects = SkillEffects()
    skillEffects.builderFunction()

    return skillEffects
}
