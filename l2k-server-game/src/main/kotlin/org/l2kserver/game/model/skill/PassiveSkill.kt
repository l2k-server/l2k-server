package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.skill.instance.PassiveSkillInstance
import org.l2kserver.game.model.skill.template.PassiveSkillTemplate

class PassiveSkill(
    entity: SkillEntity,
    private val template: PassiveSkillTemplate
): PassiveSkillInstance {
    override val skillId = entity.skillId
    override val skillLevel by entity::skillLevel
    override val skillName = template.skillName

    override fun toString() = "PassiveSkill(id=$skillId name=$skillName level=$skillLevel)"

    override fun effect(actor: ActorInstance) = template.effect(actor, skillLevel)
}
