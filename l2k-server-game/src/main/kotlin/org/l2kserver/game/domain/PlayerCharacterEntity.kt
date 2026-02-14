package org.l2kserver.game.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.datetime
import org.l2kserver.game.model.actor.character.CharacterRace
import org.l2kserver.game.model.actor.character.Gender
import java.time.LocalDateTime

object PlayerCharacterTable: IntIdTable("characters") {
    val accountName = varchar("account_name", length = 16)
    val name = varchar("name", length = 16).uniqueIndex()
    val title = varchar("title", length = 16).default("")
    val clanId = integer("clan_id").default(defaultValue = 0)
    val gender = pgEnumeration<Gender>("gender", "GENDER")
    val race = pgEnumeration<CharacterRace>("race", "RACE")
    val classId = integer("class_id")
    val currentHp = integer("current_hp")
    val currentMp = integer("current_mp")
    val currentCp = integer("current_cp")
    val sp = integer("sp").default(defaultValue = 0)
    val exp = long("exp").default(defaultValue = 0)
    val karma = integer("karma").default(defaultValue = 0)
    val pvpCount = integer("pvp_count").default(defaultValue = 0)
    val pkCount = integer("pk_count").default(defaultValue = 0)
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
    val activeSubclass = integer("active_subclass").default(defaultValue = 0)
    val accessLevel = pgEnumeration<AccessLevel>("access_level", "ACCESS_LEVEL")
        .default(AccessLevel.PLAYER)
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
