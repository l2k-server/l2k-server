package org.l2kserver.game.model.skill

import org.l2kserver.game.model.skill.instance.ToggleSkillInstance

class ToggleSkillInstanceImpl: ToggleSkillInstance {
    override val skillId: Int get() = TODO("Not yet implemented")
    override val skillName: String get() = TODO("Not yet implemented")
    override val skillLevel: Int get() = TODO("Not yet implemented")

    override fun toString() = "ToggleSkill(id=$skillId name=$skillName level=$skillLevel)"
}
