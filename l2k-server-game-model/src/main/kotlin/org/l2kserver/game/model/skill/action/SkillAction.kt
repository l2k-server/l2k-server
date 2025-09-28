package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.action.effect.SkillEffects

interface SkillAction

interface SingleTargetPhysicalSkillAction: SkillAction {
    fun applyTo(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSoulshot: Boolean
    ): SkillEffects
}

interface SingleTargetMagicSkillAction: SkillAction {

    fun applyTo(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSpiritshotType: SpiritshotType?
    ): SkillEffects

}
