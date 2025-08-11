package org.l2kserver.game.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import java.time.LocalDateTime

object PlayerCharacterTable: IntIdTable("characters") {
    val accountName = varchar("account_name", 16)
    val name = varchar("name", 16).uniqueIndex()
    val title = varchar("title", 16).default("")
    val clanId = integer("clan_id").default(0)
    val gender = postgresEnumeration<Gender>("gender", "GENDER")
    val race = postgresEnumeration<CharacterRace>("race", "RACE")
    val classId = integer("class_id")
    val currentHp = integer("current_hp")
    val currentMp = integer("current_mp")
    val currentCp = integer("current_cp")
    val sp = integer("sp").default(0)
    val exp = long("exp").default(0)
    val karma = integer("karma").default(0)
    val pvpCount = integer("pvp_count").default(0)
    val pkCount = integer("pk_count").default(0)
    val hairStyle = integer("hair_style")
    val hairColor = integer("hair_color")
    val faceType = integer("face_type")
    val lastAccess = datetime("last_access").default(LocalDateTime.now())
    val deletionDate = datetime("deletion_date").nullable()
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val nameColor = integer("name_color")
    val titleColor = integer("title_color")
    val activeSubclass = integer("active_subclass").default(0)
    val accessLevel = postgresEnumeration<AccessLevel>("access_level", "ACCESS_LEVEL").default(AccessLevel.PLAYER)
}

class PlayerCharacterEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PlayerCharacterEntity>(PlayerCharacterTable)

    var accountName by PlayerCharacterTable.accountName
    var name by PlayerCharacterTable.name

    var title by PlayerCharacterTable.title
    var clanId by PlayerCharacterTable.clanId

    var gender by PlayerCharacterTable.gender
    var race by PlayerCharacterTable.race
    var classId by PlayerCharacterTable.classId

    var currentHp by PlayerCharacterTable.currentHp
    var currentMp by PlayerCharacterTable.currentMp
    var currentCp by PlayerCharacterTable.currentCp

    var sp by PlayerCharacterTable.sp
    var exp by PlayerCharacterTable.exp
    var karma by PlayerCharacterTable.karma
    var pvpCount by PlayerCharacterTable.pvpCount
    var pkCount by PlayerCharacterTable.pkCount

    var hairStyle by PlayerCharacterTable.hairStyle
    var hairColor by PlayerCharacterTable.hairColor
    var faceType by PlayerCharacterTable.faceType

    var lastAccess by PlayerCharacterTable.lastAccess
    var deletionDate by PlayerCharacterTable.deletionDate

    var x by PlayerCharacterTable.x
    var y by PlayerCharacterTable.y
    var z by PlayerCharacterTable.z

    var nameColor by PlayerCharacterTable.nameColor
    var titleColor by PlayerCharacterTable.titleColor

    var activeSubclass by PlayerCharacterTable.activeSubclass
    var accessLevel by PlayerCharacterTable.accessLevel
}

/**
 * User's access level.
 */
enum class AccessLevel {
    /** Average player */
    PLAYER,

    /** GAME_MASTER can use admin commands and has some other privileges */
    GAME_MASTER
}
