package org.l2kserver.game.repository

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.domain.SkillTable
import org.l2kserver.game.model.actor.character.SkillToLearn
import org.l2kserver.game.model.skill.Skill
import org.l2kserver.game.model.skill.SkillTemplate
import org.springframework.stereotype.Component

@Component
class SkillRepository {

    /**
     * Saves new skill to database
     *
     * @param characterId ID of character, who has learnt this skill
     * @param subclassIndex Active subclass of character, who has learnt this skill
     * @param skillId Skill ID
     * @param skillLevel Skill level
     */
    fun save(characterId: Int, subclassIndex: Int, skillId: Int, skillLevel: Int): Skill = transaction {
        var existingSkill = SkillEntity.findByOrNull(skillId, characterId, subclassIndex)
        if (existingSkill != null) {
            existingSkill.skillLevel = skillLevel
        }
        else {
            existingSkill = SkillEntity.new {
                this.characterId = characterId
                this.subclassIndex = subclassIndex
                this.skillId = skillId
                this.skillLevel = skillLevel
            }
        }

        return@transaction existingSkill.toSkill()
    }

    /** Saves all the [skills] to DB */
    fun saveAll(characterId: Int, subclassIndex: Int, skills: List<SkillToLearn>) = transaction {
        skills.forEach { skill ->
            save(characterId, subclassIndex, skill.skillId, skill.skillLevel)
        }
    }

    /**
     * Finds learnt skill in database
     *
     * @throws IllegalArgumentException if nothing found
     */
    fun findBy(skillId: Int, characterId: Int, subclassIndex: Int): Skill = transaction {
        requireNotNull(findByOrNull(skillId, characterId, subclassIndex)) {
            "Skill '$skillId' was not learnt or does not exist"
        }
    }


    /**
     * Finds learnt skill in database, or null, if nothing found
     */
    fun findByOrNull(skillId: Int, characterId: Int, subclassIndex: Int): Skill? = transaction {
        SkillEntity.findByOrNull(skillId, characterId, subclassIndex)?.toSkill()
    }

    /** Finds all the skills learned by character for all of his subclasses**/
    fun findAllByCharacterId(characterId: Int) = transaction {
        SkillEntity.find { SkillTable.characterId eq characterId }.map(SkillEntity::toSkill)
    }


    /** Finds all the skills learned by character for provided subclass **/
    fun findAllByCharacterIdAndSubclassIndex(characterId: Int, subclassIndex: Int) = transaction {
        SkillEntity.findAllByCharacterIdAndSubclassIndex(characterId, subclassIndex)
            .map(SkillEntity::toSkill)
    }
}

private fun SkillEntity.toSkill() = Skill(this, SkillTemplate.Registry.findById(this.skillId))

private fun SkillEntity.Companion.findAllByCharacterIdAndSubclassIndex(
    characterId: Int, subclassIndex: Int
) = SkillEntity.find { (SkillTable.characterId eq characterId) and (SkillTable.subclassIndex eq subclassIndex) }

private fun SkillEntity.Companion.findByOrNull(skillId: Int, characterId: Int, subclassIndex: Int) = SkillEntity.find {
    (SkillTable.skillId eq skillId) and
            (SkillTable.characterId eq characterId) and
            (SkillTable.subclassIndex eq subclassIndex)
}.firstOrNull()
