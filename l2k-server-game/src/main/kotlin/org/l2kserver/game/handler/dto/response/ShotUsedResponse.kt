package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.item.instance.ShotInstance
import org.l2kserver.game.model.item.instance.SoulshotInstance
import org.l2kserver.game.model.item.instance.SpiritshotInstance
import org.l2kserver.game.model.item.template.Grade

private fun getSoulshotUsageSkillId(grade: Grade) = when(grade) {
    Grade.NO_GRADE -> 2039
    Grade.D -> 2150
    Grade.C -> 2151
    Grade.B -> 2152
    Grade.A -> 2153
    Grade.S -> 2154
}

private fun getSpiritshotUsageSkillId(grade: Grade) = when(grade) {
    Grade.NO_GRADE -> 2061
    Grade.D -> 2155
    Grade.C -> 2156
    Grade.B -> 2157
    Grade.A -> 2158
    Grade.S -> 2159
}

@Suppress("FunctionName")
fun ShotUsedResponse(user: PlayerCharacter, shot: ShotInstance) = SkillUsedResponse(
    casterId = user.id,
    targetId = user.id,
    skillId = when(shot) {
        is SoulshotInstance -> getSoulshotUsageSkillId(shot.grade)
        is SpiritshotInstance -> getSpiritshotUsageSkillId(shot.grade)
    },
    skillLevel = 1,
    castTime = 0,
    reuseDelay = 0,
    casterPosition = user.position
)
