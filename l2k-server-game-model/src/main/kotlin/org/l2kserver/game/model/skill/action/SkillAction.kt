package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.effect.SkillEffects

interface SkillAction

interface SingleTargetPhysicalSkillAction : SkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSoulshot: Boolean
    ): SkillEffects
}

interface SingleTargetMagicSkillAction : SkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSpiritshotType: SpiritshotType?
    ): SkillEffects
}
