package org.l2kserver.game.extensions

import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.domain.SkillsTable
import org.l2kserver.game.extensions.model.skill.toSkill

fun SkillEntity.Companion.findAllByCharacterId(characterId: Int) = this
    .find { SkillsTable.characterId eq characterId }
    .map(SkillEntity::toSkill)
