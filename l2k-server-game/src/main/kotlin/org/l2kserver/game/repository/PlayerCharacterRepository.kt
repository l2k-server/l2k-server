package org.l2kserver.game.repository

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.extensions.model.item.createAllFrom
import org.l2kserver.game.extensions.model.shortcut.createAllFrom
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.L2kCharacterClass
import org.l2kserver.game.model.item.Item
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private const val DEFAULT_NAME_COLOR = 0xFFFFFF
private const val DEFAULT_TITLE_COLOR = 0xFFFF77
private const val DEFAULT_TITLE = ""

@Component
class PlayerCharacterRepository {

    private val log = logger()

    fun create(
        accountName: String, characterName: String, race: CharacterRace, gender: Gender,
        classId: Int, hairColor: Int, hairStyle: Int, faceType: Int
    ): PlayerCharacter = transaction {
        val characterClass = requireNotNull(L2kCharacterClass.Registry.findById(classId)) {
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
            this.x = characterTemplate.position.x
            this.y = characterTemplate.position.y
            this.z = characterTemplate.position.z
            this.nameColor = DEFAULT_NAME_COLOR
            this.titleColor = DEFAULT_TITLE_COLOR
            this
        }

        Item.createAllFrom(characterEntity.id.value, characterTemplate.items)
        Shortcut.createAllFrom(characterEntity.id.value, characterTemplate.shortcuts)

        val character = characterEntity.toPlayerCharacter()!!

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
            .where { (PlayerCharacterTable.accountName eq accountName) and (PlayerCharacterTable.deletionDate neq null) }
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

    private fun PlayerCharacterEntity.toPlayerCharacter(): PlayerCharacter? {
        val characterClass = L2kCharacterClass.Registry.findById(this.classId)
        return if (characterClass != null)
            PlayerCharacter(this, characterClass)
        else {
            log.warn("No character class exists by Id")
            null
        }
    }

}
