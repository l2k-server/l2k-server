package org.l2kserver.game.extensions

import org.l2kserver.game.domain.LearnedSkillEntity
import org.l2kserver.game.domain.LearnedSkillsTable
import org.l2kserver.game.extensions.model.skill.toSkill

fun LearnedSkillEntity.Companion.findAllByCharacterId(characterId: Int) = this
    .find { LearnedSkillsTable.characterId eq characterId }
    .map(LearnedSkillEntity::toSkill)
