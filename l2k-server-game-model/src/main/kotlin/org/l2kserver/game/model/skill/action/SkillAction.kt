package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.action.effect.DamageEffect
import org.l2kserver.game.model.skill.action.effect.Effect

@JvmInline
value class SkillEffects private constructor(
    private val effectList: MutableList<Effect>
): Iterable<Effect> by effectList {
    constructor(): this(ArrayList<Effect>())

    fun add(effect: Effect) {
        effectList.add(effect)
    }

    /** Applies the event of dealing damage */
    fun hit(
        damage: Int,
        isCritical: Boolean = false,
        isBlocked: Boolean = false,
        isMagicCritical: Boolean = false,
        isHalfSuccessful: Boolean = false,
        isFailed: Boolean = false
    ) = add(
        DamageEffect(
            damage,
            isCritical = isCritical,
            isBlocked = isBlocked,
            isMagicCritical = isMagicCritical,
            isHalfSuccessful = isHalfSuccessful,
            isFailed = isFailed
        )
    )

    /** Applies the event of missing target */
    fun miss() = add(DamageEffect(isAvoided = true))

}

interface SkillAction

interface SingleTargetPhysicalSkillAction: SkillAction {

    val overhitPossible: Boolean get() = false

    fun applyTo(target: ActorInstance, caster: ActorInstance, actionLevel: Int, usedSoulshot: Boolean): SkillEffects
}

interface SingleTargetMagicSkillAction: SkillAction {
    fun applyTo(target: ActorInstance, caster: ActorInstance, actionLevel: Int /** usedSpiritshotType */): SkillEffects
}

inline fun effects(builderFunction: SkillEffects.() -> Unit): SkillEffects {
    val skillEffects = SkillEffects()
    skillEffects.builderFunction()

    return skillEffects
}
