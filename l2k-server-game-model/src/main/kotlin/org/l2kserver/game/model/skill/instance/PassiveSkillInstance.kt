package org.l2kserver.game.model.skill.instance

import org.l2kserver.game.model.skill.action.AbnormalSkillAction

interface PassiveSkillInstance: SkillInstance {
    override val skillId: Int
    override val skillName: String
    override val skillLevel: Int

    val skillAction: AbnormalSkillAction
}
