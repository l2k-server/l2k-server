package org.l2kserver.game.model.skill.instance

sealed interface SkillInstance {
    val skillId: Int
    val skillName: String
    val skillLevel: Int
}
