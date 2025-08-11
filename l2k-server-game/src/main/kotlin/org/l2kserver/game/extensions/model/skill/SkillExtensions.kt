package org.l2kserver.game.extensions.model.skill

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.upsert
import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.domain.SkillsTable
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.model.skill.Skill

fun SkillEntity.toSkill() = Skill(this, SkillTemplate.Registry.findById(this.skillId)!!) //TODO in not found?

fun Skill.Companion.findAllByCharacterIdAndSubclassIndex(characterId: Int, vararg subclassIndices: Int?) = SkillEntity
    .find { (SkillsTable.characterId eq characterId) and (SkillsTable.subclassIndex inList subclassIndices.asList()) }
    .map(SkillEntity::toSkill)


/**
 * Finds learnt skill in database
 *
 * @throws IllegalArgumentException if nothing found
 */
fun Skill.Companion.findBy(skillId: Int, characterId: Int, subclassIndex: Int) =
    requireNotNull(Skill.findByOrNull(skillId, characterId, subclassIndex)) {
        "Skill '$skillId' was not learnt or does not exist"
    }

/**
 * Finds learnt skill in database, or null, if nothing found
 */
fun Skill.Companion.findByOrNull(skillId: Int, characterId: Int, subclassIndex: Int): Skill? = SkillEntity
    .find {
        (SkillsTable.skillId eq skillId) and
                (SkillsTable.characterId eq characterId) and
                (SkillsTable.subclassIndex eq subclassIndex)
    }
    .firstOrNull()
    ?.toSkill()

/**
 * Saves new skill to database
 *
 * @param characterId ID of character, who has learnt this skill
 * @param subclassIndex Active subclass of character, who has learnt this skill
 * @param skillId Skill ID
 * @param skillLevel Skill level
 */
fun Skill.Companion.create(
    characterId: Int,
    subclassIndex: Int,
    skillId: Int,
    skillLevel: Int
): Skill {
    SkillsTable.upsert { statement ->
        statement[SkillsTable.characterId] = characterId
        statement[SkillsTable.subclassIndex] = subclassIndex
        statement[SkillsTable.skillId] = skillId
        statement[SkillsTable.skillLevel] = skillLevel
    }

    return Skill.findBy(skillId, characterId, subclassIndex)
}
