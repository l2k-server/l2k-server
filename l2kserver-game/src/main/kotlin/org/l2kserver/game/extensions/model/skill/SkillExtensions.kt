package org.l2kserver.game.extensions.model.skill

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.upsert
import org.l2kserver.game.domain.LearnedSkillEntity
import org.l2kserver.game.domain.LearnedSkillsTable
import org.l2kserver.game.model.skill.SkillTemplate
import org.l2kserver.game.model.skill.Skill

fun LearnedSkillEntity.toSkill() = Skill(this, SkillTemplate.Registry.findById(this.skillId)!!) //TODO in not found?

fun Skill.Companion.findAllByCharacterIdAndSubclassIndex(characterId: Int, subclassIndex: Int) = LearnedSkillEntity
    .find { (LearnedSkillsTable.characterId eq characterId) and (LearnedSkillsTable.subclassIndex eq subclassIndex) }
    .map(LearnedSkillEntity::toSkill)


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
fun Skill.Companion.findByOrNull(skillId: Int, characterId: Int, subclassIndex: Int): Skill? = LearnedSkillEntity
    .find {
        (LearnedSkillsTable.skillId eq skillId) and
                (LearnedSkillsTable.characterId eq characterId) and
                (LearnedSkillsTable.subclassIndex eq subclassIndex)
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
fun Skill.Companion.save(
    characterId: Int,
    subclassIndex: Int,
    skillId: Int,
    skillLevel: Int
): Skill {
    LearnedSkillsTable.upsert { statement ->
        statement[LearnedSkillsTable.characterId] = characterId
        statement[LearnedSkillsTable.subclassIndex] = subclassIndex
        statement[LearnedSkillsTable.skillId] = skillId
        statement[LearnedSkillsTable.skillLevel] = skillLevel
    }

    return Skill.findBy(skillId, characterId, subclassIndex)
}
