package org.l2kserver.game.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object SkillsTable: IntIdTable("skills") {
    val characterId = integer("character_id")
    val subclassIndex = integer("subclass_index").nullable()
    val skillId = integer("skill_id")
    val skillLevel = integer("skill_level")
    val nextUsageTime = timestamp("next_usage_time")
    //TODO skill enchantments
}

class SkillEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SkillEntity>(SkillsTable)

    val characterId by SkillsTable.characterId
    val subclassIndex by SkillsTable.subclassIndex
    val skillId by SkillsTable.skillId
    val skillLevel by SkillsTable.skillLevel
    var nextUsageTime by SkillsTable.nextUsageTime
}
