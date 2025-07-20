package org.l2kserver.game.domain.skill

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object LearnedSkillsTable: IntIdTable("learned_skills") {
    val characterId = integer("character_id")
    val subclassIndex = integer("subclass_index")
    val skillId = integer("skill_id")
    val skillLevel = integer("skill_level")
    //TODO skill enchantments
}

class LearnedSkillEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LearnedSkillEntity>(LearnedSkillsTable)

    val characterId by LearnedSkillsTable.characterId
    val subclassIndex by LearnedSkillsTable.subclassIndex
    val skillId by LearnedSkillsTable.skillId
    val skillLevel by LearnedSkillsTable.skillLevel
}
