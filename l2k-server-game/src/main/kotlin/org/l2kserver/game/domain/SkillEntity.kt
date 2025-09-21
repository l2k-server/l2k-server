package org.l2kserver.game.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object SkillTable: IntIdTable("skills") {
    val characterId = integer("character_id")
    val subclassIndex = integer("subclass_index").nullable()
    val skillId = integer("skill_id")
    val skillLevel = integer("skill_level")
    //TODO skill enchantments
}

class SkillEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SkillEntity>(SkillTable)

    var characterId by SkillTable.characterId
    var subclassIndex by SkillTable.subclassIndex
    var skillId by SkillTable.skillId
    var skillLevel by SkillTable.skillLevel
}
