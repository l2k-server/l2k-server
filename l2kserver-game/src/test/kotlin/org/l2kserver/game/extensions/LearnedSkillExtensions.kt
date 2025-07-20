package org.l2kserver.game.extensions

import org.l2kserver.game.domain.skill.LearnedSkillEntity
import org.l2kserver.game.domain.skill.LearnedSkillsTable
import org.l2kserver.game.extensions.model.skill.toSkill

fun LearnedSkillEntity.Companion.findAllByCharacterId(characterId: Int) = this
    .find { LearnedSkillsTable.characterId eq characterId }
    .map(LearnedSkillEntity::toSkill)
