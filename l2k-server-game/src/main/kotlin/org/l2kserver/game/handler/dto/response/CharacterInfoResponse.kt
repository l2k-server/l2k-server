package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toByte
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.Posture
import kotlin.math.roundToInt

private const val CHARACTER_INFO_RESPONSE_PACKET_ID: UByte = 3u

//TODO If character is morphed

/**
 * Character info, which is sent to all players who see this character
 */
data class CharacterInfoResponse(
    val character: PlayerCharacterInstanceImpl
) : ResponsePacket {

    override val data = littleEndianByteArray {
        // Speed multiplier. Client needs it for proper animations
        val speedMultiplier =
            character.stats.speed.toDouble() / character.characterClass.baseSpeed
        // Attack speed multiplier. Client needs it for proper animations
        val atkSpeedMultiplier =
            character.stats.atkSpd.toDouble() / character.characterClass.baseAtkSpd

        putUByte(CHARACTER_INFO_RESPONSE_PACKET_ID)

        putInt(character.position.x)
        putInt(character.position.y)
        putInt(character.position.z)
        putInt(character.heading.toInt())

        putInt(character.id)
        putUTF16String(character.name)
        putInt(character.race.ordinal)
        putInt(character.gender.ordinal)
        putInt(character.characterClass.baseClassId)

        putInt(character.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(character.inventory.headgear?.templateId ?: 0)
        putInt(character.inventory.oneHanded?.templateId ?: 0)
        putInt(character.inventory.shield?.templateId ?: 0)
        putInt(character.inventory.gloves?.templateId ?: 0)
        putInt(character.inventory.upperBody?.templateId ?: 0)
        putInt(character.inventory.lowerBody?.templateId ?: 0)
        putInt(character.inventory.boots?.templateId ?: 0)
        putInt(character.inventory.underwear?.templateId ?: 0)
        putInt(character.inventory.twoHanded?.templateId ?: 0)
        putInt(character.inventory.hairAccessory?.templateId ?:
            character.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(character.inventory.faceAccessory?.templateId ?: 0)

        put(ByteArray(8))
        putInt(character.inventory.weapon?.augmentationId ?: 0)
        put(ByteArray(24))
        putInt(character.inventory.weapon?.augmentationId ?: 0)
        put(ByteArray(8))

        putInt(character.pvpState.ordinal)
        putInt(character.karma)

        putInt(character.stats.castingSpd)
        putInt(character.stats.atkSpd)

        putInt(character.pvpState.ordinal)
        putInt(character.karma)

        putInt((character.stats.speed / speedMultiplier).toInt())
        putInt((character.stats.walkSpeed / speedMultiplier).toInt())
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO Swim run speed
        putInt((character.stats.walkSpeed / speedMultiplier).toInt()) //TODO Swim walk speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fl (??) run speed
        putInt((character.stats.walkSpeed / speedMultiplier).toInt()) //TODO fl (??) walk speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fly run speed
        putInt((character.stats.walkSpeed / speedMultiplier).toInt()) //TODO fly walk speed
        putDouble(speedMultiplier)
        putDouble(atkSpeedMultiplier)

        //TODO Hitbox should be taken from pet if mounted
        putDouble(character.collisionBox.radius)
        putDouble(character.collisionBox.height)

        putInt(character.hairStyle)
        putInt(character.hairColor)
        putInt(character.faceType)

        putUTF16String(character.title)

        putInt(character.clanId)
        putInt(0) //TODO Clan crest id
        putInt(0) //TODO Alliance id
        putInt(0) //TODO Alliance crest id
        putInt(0) //TODO Siege relation

        put((character.posture == Posture.STANDING).toByte())
        put((character.moveType == MoveType.RUN).toByte())

        put(character.isFighting.toByte())
        put(character.isDead().toByte()) //TODO fake death

        put(0) //TODO Is invisible

        put(0) //TODO Mount type. 0 - none, 1 - strider, 2 - wyvern

        put(character.privateStore?.storeType?.toByte() ?: 0)

        //TODO Cubics - first goes cubics amount, then their ids
        putShort(0) //cubic amount
        //.writeShortLE(cubic.id) cubic ids

        put(0) //TODO (L2J) Find party members (?)

        putInt(character.temporalEffects.visible)

        put(0) //TODO Recommendations Left
        putShort(0) // TODO Recommendations Received Blue value for name (0 = white, 255 = pure blue)

        putInt(character.characterClass.id)

        putInt(character.stats.maxCp.roundToInt())
        putInt(character.currentCp)

        put(minOf(16, character.inventory.weapon?.enchantLevel ?: 0).toByte())

        put(0) //TODO team circle around feet 1 = Blue, 2 = red

        putInt(0) //TODO getClanCrestLargeId

        put(0) //TODO isNoble
        put(0) //TODO isHero

        put(0) //TODO isFishing
        putInt(0) //TODO Fish X
        putInt(0) //TODO Fish Y
        putInt(0) //TODO Fish Z

        putInt(character.nameColor)
        putInt(0)

        putInt(0) //TODO PledgeClass(?)
        putInt(0)

        putInt(character.titleColor)
        putInt(0) //TODO Cursed weapon level
    }

}
