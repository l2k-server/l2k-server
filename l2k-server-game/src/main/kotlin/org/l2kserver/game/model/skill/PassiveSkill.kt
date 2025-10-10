package org.l2kserver.game.model.skill

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.model.skill.instance.PassiveSkillInstance
import org.l2kserver.game.model.skill.template.PassiveSkillTemplate

class PassiveSkill(
    entity: SkillEntity,
    template: PassiveSkillTemplate
): PassiveSkillInstance {
    override val skillId = entity.skillId
    override val skillLevel by entity::skillLevel
    override val skillName = template.skillName
    override val skillAction = template.skillAction

    override fun toString() = "PassiveSkill(id=$skillId name=$skillName level=$skillLevel)"
}
