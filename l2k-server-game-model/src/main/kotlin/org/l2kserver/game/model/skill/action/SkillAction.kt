package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.effect.Effects

sealed interface SkillAction

interface ActiveSkillAction: SkillAction

fun interface SingleTargetPhysicalSkillAction : ActiveSkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSoulshot: Boolean
    ): Effects
}

fun interface SingleTargetMagicSkillAction : ActiveSkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSpiritshotType: SpiritshotType?
    ): Effects
}
