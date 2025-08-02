package org.l2kserver.game.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.l2kserver.game.model.actor.AccessLevel
import org.l2kserver.game.model.actor.character.CharacterClassName
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender

object PlayerCharacterTable: IntIdTable("characters") {
    val accountName = varchar("account_name", 16)
    val name = varchar("name", 16).uniqueIndex()
    val title = varchar("title", 16)
    val clanId = integer("clan_id")
    val gender = postgresEnumeration<Gender>("gender", "GENDER")
    val race = postgresEnumeration<CharacterRace>("race", "RACE")
    val className = postgresEnumeration<CharacterClassName>("class_name", "CHARACTER_CLASS")
    val currentHp = integer("current_hp")
    val currentMp = integer("current_mp")
    val currentCp = integer("current_cp")
    val sp = integer("sp")
    val exp = long("exp")
    val karma = integer("karma")
    val pvpCount = integer("pvp_count")
    val pkCount = integer("pk_count")
    val hairStyle = integer("hair_style")
    val hairColor = integer("hair_color")
    val faceType = integer("face_type")
    val lastAccess = datetime("last_access")
    val deletionDate = datetime("deletion_date").nullable()
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val nameColor = integer("name_color")
    val titleColor = integer("title_color")
    val activeSubclass = integer("active_subclass")
    val accessLevel = postgresEnumeration<AccessLevel>("access_level", "ACCESS_LEVEL")
}

class PlayerCharacterEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PlayerCharacterEntity>(PlayerCharacterTable)

    val accountName by PlayerCharacterTable.accountName
    val name by PlayerCharacterTable.name

    var title by PlayerCharacterTable.title
    var clanId by PlayerCharacterTable.clanId

    val gender by PlayerCharacterTable.gender
    val race by PlayerCharacterTable.race
    val className by PlayerCharacterTable.className

    var currentHp by PlayerCharacterTable.currentHp
    var currentMp by PlayerCharacterTable.currentMp
    var currentCp by PlayerCharacterTable.currentCp

    var sp by PlayerCharacterTable.sp
    var exp by PlayerCharacterTable.exp
    var karma by PlayerCharacterTable.karma
    var pvpCount by PlayerCharacterTable.pvpCount
    var pkCount by PlayerCharacterTable.pkCount

    val hairStyle by PlayerCharacterTable.hairStyle
    val hairColor by PlayerCharacterTable.hairColor
    val faceType by PlayerCharacterTable.faceType

    var lastAccess by PlayerCharacterTable.lastAccess
    var deletionDate by PlayerCharacterTable.deletionDate

    var x by PlayerCharacterTable.x
    var y by PlayerCharacterTable.y
    var z by PlayerCharacterTable.z

    val nameColor by PlayerCharacterTable.nameColor
    val titleColor by PlayerCharacterTable.titleColor

    var activeSubclass by PlayerCharacterTable.activeSubclass
    var accessLevel by PlayerCharacterTable.accessLevel
}
