package org.l2kserver.game.model.skill.action

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.character.CharacterInstance
import org.l2kserver.game.model.item.template.SpiritshotType
import org.l2kserver.game.model.skill.effect.SkillEffects

sealed interface SkillAction

fun interface AbnormalSkillAction: SkillAction {
    fun apply(character: CharacterInstance, actionLevel: Int): SkillEffects
}

interface ActiveSkillAction: SkillAction

fun interface SingleTargetPhysicalSkillAction : ActiveSkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSoulshot: Boolean
    ): SkillEffects
}

fun interface SingleTargetMagicSkillAction : ActiveSkillAction {
    fun apply(
        target: ActorInstance,
        caster: ActorInstance,
        actionLevel: Int,
        usedSpiritshotType: SpiritshotType?
    ): SkillEffects
}
