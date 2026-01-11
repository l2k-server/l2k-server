package org.l2kserver.game.repository

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.l2kserver.game.domain.ItemEntity
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.domain.SkillEntity
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.CharacterClassRegistry
import org.l2kserver.game.utils.LevelUtils
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private const val DEFAULT_NAME_COLOR = 0xFFFFFF
private const val DEFAULT_TITLE_COLOR = 0xFFFF77
private const val DEFAULT_TITLE = ""

@Component
class PlayerCharacterRepository(
    private val shortcutRepository: ShortcutRepository
) {

    private val log = logger()

    fun create(
        accountName: String, characterName: String, race: CharacterRace, gender: Gender,
        classId: Int, hairColor: Int, hairStyle: Int, faceType: Int
    ): PlayerCharacterInstanceImpl = transaction {
        val characterClass = requireNotNull(CharacterClassRegistry.findByIdOrNull(classId)) {
            "No class with id $classId exists!"
        }

        val characterTemplate = requireNotNull(characterClass.characterTemplate) {
            "Character of class $classId cannot be created!"
        }

        val characterEntity = PlayerCharacterEntity.new {
            this.accountName = accountName
            this.name = characterName
            this.title = DEFAULT_TITLE
            this.gender = gender
            this.race = race
            this.classId = classId
            this.currentCp = 1
            this.currentHp = 1
            this.currentMp = 1
            this.hairStyle = hairStyle
            this.hairColor = hairColor
            this.faceType = faceType
            this.lastAccess = LocalDateTime.now()
            this.x = characterTemplate.position.x
            this.y = characterTemplate.position.y
            this.z = characterTemplate.position.z
            this.nameColor = DEFAULT_NAME_COLOR
            this.titleColor = DEFAULT_TITLE_COLOR
        }

        val characterLevel = LevelUtils.getLevelByExp(characterEntity.exp)

        for ((requiredLevel, skillsToLearn) in characterClass.skillTree) {
            if (requiredLevel > characterLevel) continue

            skillsToLearn.filter { it.autoLearn }.forEach {
                SkillEntity.new {
                    this.characterId = characterEntity.id.value
                    this.subclassIndex = characterEntity.activeSubclass
                    this.skillId = it.skillId
                    this.skillLevel = it.skillLevel
                }
            }
        }

        ItemEntity.createAllFrom(characterEntity.id.value, characterTemplate.items)

        val character = characterEntity.toPlayerCharacter()!!

        shortcutRepository.createAllFrom(
            characterTemplate.shortcuts,
            character
        )

        character.currentCp = character.stats.maxCp
        character.currentHp = character.stats.maxHp
        character.currentMp = character.stats.maxMp

        return@transaction character
    }

    fun findById(characterId: Int) = transaction {
        PlayerCharacterEntity.findById(characterId)?.toPlayerCharacter()
    }

    fun findAllByAccountName(accountName: String) = transaction {
        PlayerCharacterEntity
            .find { PlayerCharacterTable.accountName eq accountName }
            .orderBy(PlayerCharacterTable.lastAccess to SortOrder.DESC)
            .mapNotNull { it.toPlayerCharacter() }
    }

    fun countByAccountName(accountName: String) = transaction {
        PlayerCharacterTable.selectAll()
            .where { PlayerCharacterTable.accountName eq accountName }
            .count()
    }

    fun existsByName(characterName: String) = transaction {
        PlayerCharacterTable
            .select(listOf(PlayerCharacterTable.id))
            .where { PlayerCharacterTable.name eq characterName }
            .count() > 0
    }

    fun existDeletingByAccountName(accountName: String) = transaction {
        PlayerCharacterTable
            .select(listOf(PlayerCharacterTable.id))
            .where {
                (PlayerCharacterTable.accountName eq accountName) and (PlayerCharacterTable.deletionDate neq null)
            }
            .count() > 0
    }

    /**
     * Deletes all the characters with expired deletion datetime
     *
     * @return Deleted characters
     */
    fun deleteAllWithExpiredDeletionDate() = transaction {
        PlayerCharacterTable
            .deleteReturning { PlayerCharacterTable.deletionDate lessEq LocalDateTime.now() }
            .mapNotNull { PlayerCharacterEntity.wrapRow(it).toPlayerCharacter() }
    }

    private fun PlayerCharacterEntity.toPlayerCharacter(): PlayerCharacterInstanceImpl? {
        val characterClass = CharacterClassRegistry.findByIdOrNull(this.classId)
        return if (characterClass != null)
            PlayerCharacterInstanceImpl(this, characterClass)
        else {
            log.warn("No character class exists by Id")
            null
        }
    }

}
