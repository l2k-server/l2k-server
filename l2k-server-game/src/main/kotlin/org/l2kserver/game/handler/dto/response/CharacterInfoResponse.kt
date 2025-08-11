package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toByte
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.Posture

private const val CHARACTER_INFO_RESPONSE_PACKET_ID: UByte = 3u

//TODO If character is morphed

/**
 * Character info, which is sent to all players who see this character
 */
data class CharacterInfoResponse(
    val playerCharacter: PlayerCharacter
) : ResponsePacket {

    override val data = littleEndianByteArray {
        // Speed multiplier. Client needs it for proper animations
        val speedMultiplier =
            playerCharacter.stats.speed.toDouble() / playerCharacter.characterClass.baseSpeed
        // Attack speed multiplier. Client needs it for proper animations
        val atkSpeedMultiplier =
            playerCharacter.stats.atkSpd.toDouble() / playerCharacter.characterClass.baseAtkSpd

        putUByte(CHARACTER_INFO_RESPONSE_PACKET_ID)

        putInt(playerCharacter.position.x)
        putInt(playerCharacter.position.y)
        putInt(playerCharacter.position.z)
        putInt(playerCharacter.heading.toInt())

        putInt(playerCharacter.id)
        putUTF16String(playerCharacter.name)
        putInt(playerCharacter.race.ordinal)
        putInt(playerCharacter.gender.ordinal)
        putInt(playerCharacter.characterClass.baseClassId)

        putInt(playerCharacter.paperDoll.twoSlotsAccessory?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.headgear?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.oneHanded?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.shield?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.gloves?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.upperBody?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.lowerBody?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.boots?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.underwear?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.twoHanded?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.hairAccessory?.templateId ?:
            playerCharacter.paperDoll.twoSlotsAccessory?.templateId ?: 0)
        putInt(playerCharacter.paperDoll.faceAccessory?.templateId ?: 0)

        put(ByteArray(8))
        putInt(playerCharacter.paperDoll.getWeapon()?.augmentationId ?: 0)
        put(ByteArray(24))
        putInt(playerCharacter.paperDoll.getWeapon()?.augmentationId ?: 0)
        put(ByteArray(8))

        putInt(playerCharacter.pvpState.ordinal)
        putInt(playerCharacter.karma)

        putInt(playerCharacter.stats.castingSpd)
        putInt(playerCharacter.stats.atkSpd)

        putInt(playerCharacter.pvpState.ordinal)
        putInt(playerCharacter.karma)

        putInt((playerCharacter.stats.speed / speedMultiplier).toInt())
        putInt((playerCharacter.stats.walkSpeed / speedMultiplier).toInt())
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO Swim run speed
        putInt((playerCharacter.stats.walkSpeed / speedMultiplier).toInt()) //TODO Swim walk speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fl (??) run speed
        putInt((playerCharacter.stats.walkSpeed / speedMultiplier).toInt()) //TODO fl (??) walk speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fly run speed
        putInt((playerCharacter.stats.walkSpeed / speedMultiplier).toInt()) //TODO fly walk speed
        putDouble(speedMultiplier)
        putDouble(atkSpeedMultiplier)

        //TODO Hitbox should be taken from pet if mounted
        putDouble(playerCharacter.collisionBox.radius)
        putDouble(playerCharacter.collisionBox.height)

        putInt(playerCharacter.hairStyle)
        putInt(playerCharacter.hairColor)
        putInt(playerCharacter.faceType)

        putUTF16String(playerCharacter.title)

        putInt(playerCharacter.clanId)
        putInt(0) //TODO Clan crest id
        putInt(0) //TODO Alliance id
        putInt(0) //TODO Alliance crest id
        putInt(0) //TODO Siege relation

        put((playerCharacter.posture == Posture.STANDING).toByte())
        put((playerCharacter.moveType == MoveType.RUN).toByte())

        put(playerCharacter.isFighting.toByte())
        put(playerCharacter.isDead().toByte()) //TODO fake death

        put(0) //TODO Is invisible

        put(0) //TODO Mount type. 0 - none, 1 - strider, 2 - wyvern

        put(playerCharacter.privateStore?.storeType?.toByte() ?: 0)

        //TODO Cubics - first goes cubics amount, then their ids
        putShort(0) //cubic amount
        //.writeShortLE(cubic.id) cubic ids

        put(0) //TODO (L2J) Find party members (?)

        putInt(0) //TODO Abnormal Effects - stunned, rooted, etc in .

        put(0) //TODO Recommendations Left
        putShort(0) // TODO Recommendations Received Blue value for name (0 = white, 255 = pure blue)

        putInt(playerCharacter.characterClass.id)

        putInt(playerCharacter.stats.maxCp)
        putInt(playerCharacter.currentCp)

        put(minOf(16, playerCharacter.paperDoll.getWeapon()?.enchantLevel ?: 0).toByte())

        put(0) //TODO team circle around feet 1 = Blue, 2 = red

        putInt(0) //TODO getClanCrestLargeId

        put(0) //TODO isNoble
        put(0) //TODO isHero

        put(0) //TODO isFishing
        putInt(0) //TODO Fish X
        putInt(0) //TODO Fish Y
        putInt(0) //TODO Fish Z

        putInt(playerCharacter.nameColor)
        putInt(0)

        putInt(0) //TODO PledgeClass(?)
        putInt(0)

        putInt(playerCharacter.titleColor)
        putInt(0) //TODO Cursed weapon level
    }

}
