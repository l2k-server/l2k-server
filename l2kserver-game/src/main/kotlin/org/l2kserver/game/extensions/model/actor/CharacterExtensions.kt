package org.l2kserver.game.extensions.model.actor

import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteReturning
import org.jetbrains.exposed.sql.insertAndGetId
import org.l2kserver.game.domain.PlayerCharacterEntity
import org.l2kserver.game.domain.PlayerCharacterTable
import org.l2kserver.game.domain.Shortcut
import org.l2kserver.game.extensions.model.item.createAllFrom
import org.l2kserver.game.extensions.model.shortcut.create
import org.l2kserver.game.model.actor.AccessLevel
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import org.l2kserver.game.model.actor.character.CharacterClass
import org.l2kserver.game.model.actor.character.CharacterClassName
import org.l2kserver.game.model.actor.character.CharacterTemplate
import org.l2kserver.game.model.item.Item
import kotlin.math.roundToInt

fun PlayerCharacterEntity.toPlayerCharacter() = PlayerCharacter(this, CharacterClass.Registry.findByName(this.className))

fun PlayerCharacter.Companion.create(
    accountName: String,
    characterName: String,
    race: CharacterRace,
    gender: Gender,
    className: CharacterClassName,
    hairColor: Int,
    hairStyle: Int,
    faceType: Int
): Int {
    val characterTemplate = CharacterTemplate.Registry.findByClassName(className)
    val characterClass = CharacterClass.Registry.findByName(className)

    val characterId = PlayerCharacterTable.insertAndGetId { statement ->
        statement[PlayerCharacterTable.accountName] = accountName
        statement[name] = characterName
        statement[title] = ""
        statement[clanId] = 0
        statement[PlayerCharacterTable.gender] = gender
        statement[PlayerCharacterTable.race] = race
        statement[PlayerCharacterTable.className] = className
        statement[currentCp] =
            (characterClass.initialBasicStats.con.cpModifier * characterClass.initialStats.maxCp).roundToInt()
        statement[currentHp] =
            (characterClass.initialBasicStats.con.hpModifier * characterClass.initialStats.maxHp).roundToInt()
        statement[currentMp] =
            (characterClass.initialBasicStats.men.mpModifier * characterClass.initialStats.maxMp).roundToInt()
        statement[sp] = 0
        statement[exp] = 0
        statement[karma] = 0
        statement[pvpCount] = 0
        statement[pkCount] = 0
        statement[PlayerCharacterTable.hairStyle] = hairStyle
        statement[PlayerCharacterTable.hairColor] = hairColor
        statement[PlayerCharacterTable.faceType] = faceType
        statement[lastAccess] = LocalDateTime.now()
        statement[x] = characterTemplate.position.x
        statement[y] = characterTemplate.position.y
        statement[z] = characterTemplate.position.z
        statement[nameColor] = DEFAULT_NAME_COLOR
        statement[titleColor] = DEFAULT_TITLE_COLOR
        statement[activeSubclass] = 0
        statement[accessLevel] = AccessLevel.PLAYER
    }.value

    Item.createAllFrom(characterId, characterTemplate.items)

    characterTemplate.shortcuts.forEach {
        Shortcut.create(
            characterId = characterId,
            subclassIndex = 0,
            shortcutIndex = it.index,
            shortcutType = it.type,
            shortcutActionId = it.shortcutActionId,
            shortcutActionLevel = it.actionLevel
        )
    }

    return characterId
}

fun PlayerCharacter.Companion.findAllByAccountName(accountName: String) = PlayerCharacterEntity
    .find { PlayerCharacterTable.accountName eq accountName }
    .orderBy(PlayerCharacterTable.id to SortOrder.ASC)
    .map { it.toPlayerCharacter() }

fun PlayerCharacter.Companion.countByAccountName(accountName: String) = PlayerCharacterTable
    .select(listOf(PlayerCharacterTable.id))
    .where { PlayerCharacterTable.accountName eq accountName }
    .count()

fun PlayerCharacter.Companion.existDeletingByAccountName(accountName: String) = PlayerCharacterTable
    .select(listOf(PlayerCharacterTable.id))
    .where { (PlayerCharacterTable.accountName eq accountName) and (PlayerCharacterTable.deletionDate neq null) }
    .count() > 0

fun PlayerCharacter.Companion.existsByName(characterName: String) = PlayerCharacterTable
    .select(listOf(PlayerCharacterTable.id))
    .where { PlayerCharacterTable.name eq characterName }
    .count() > 0

/**
 * Deletes all the characters with expired deletion datetime
 *
 * @return Deleted characters
 */
fun PlayerCharacter.Companion.deleteAllWithExpiredDeletionDate() = PlayerCharacterTable
    .deleteReturning { PlayerCharacterTable.deletionDate lessEq LocalDateTime.now() }
    .map { PlayerCharacterEntity.wrapRow(it).toPlayerCharacter() }
