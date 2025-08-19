package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.extensions.toByte
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.MoveType

private const val CHARACTER_INFO_RESPONSE_PACKET_ID: UByte = 4u

/**
 * Full character info, which is sent to a player who plays this character
 */
data class FullCharacterResponse(
    val playerCharacter: PlayerCharacter
): ResponsePacket {

    @Suppress("LongMethod")
    override val data = littleEndianByteArray {
        // Speed multiplier. Client needs it for proper animations
        val speedMultiplier = playerCharacter.stats.speed.toDouble() / playerCharacter.characterClass.baseSpeed
        // Attack speed multiplier. Client needs it for proper animations
        val atkSpeedMultiplier = playerCharacter.stats.atkSpd.toDouble() / playerCharacter.characterClass.baseAtkSpd

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

        putInt(playerCharacter.level)
        putLong(playerCharacter.exp)

        putInt(playerCharacter.basicStats.str.value)
        putInt(playerCharacter.basicStats.dex.value)
        putInt(playerCharacter.basicStats.con.value)
        putInt(playerCharacter.basicStats.int.value)
        putInt(playerCharacter.basicStats.wit.value)
        putInt(playerCharacter.basicStats.men.value)

        putInt(playerCharacter.stats.maxHp)
        putInt(playerCharacter.currentHp)
        putInt(playerCharacter.stats.maxMp)
        putInt(playerCharacter.currentMp)

        putInt(playerCharacter.sp)

        putInt(playerCharacter.inventory.weight)
        putInt(playerCharacter.tradeAndInventoryStats.weightLimit)

        putInt(40)

        putInt(playerCharacter.inventory.twoSlotsAccessory?.id ?: 0)
        putInt(playerCharacter.inventory.leftEarring?.id ?: 0)
        putInt(playerCharacter.inventory.rightEarring?.id ?: 0)
        putInt(playerCharacter.inventory.necklace?.id ?: 0)
        putInt(playerCharacter.inventory.leftRing?.id ?: 0)
        putInt(playerCharacter.inventory.rightRing?.id ?: 0)
        putInt(playerCharacter.inventory.headgear?.id ?: 0)
        putInt(playerCharacter.inventory.oneHanded?.id ?: 0)
        putInt(playerCharacter.inventory.shield?.id ?: 0)
        putInt(playerCharacter.inventory.gloves?.id ?: 0)
        putInt(playerCharacter.inventory.upperBody?.id ?: 0)
        putInt(playerCharacter.inventory.lowerBody?.id ?: 0)
        putInt(playerCharacter.inventory.boots?.id ?: 0)
        putInt(playerCharacter.inventory.underwear?.id ?: 0)
        putInt(playerCharacter.inventory.twoHanded?.id ?: 0)
        putInt(playerCharacter.inventory.hairAccessory?.id
            ?: playerCharacter.inventory.twoSlotsAccessory?.id ?: 0)
        putInt(playerCharacter.inventory.faceAccessory?.id ?: 0)

        putInt(playerCharacter.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(playerCharacter.inventory.leftEarring?.templateId ?: 0)
        putInt(playerCharacter.inventory.rightEarring?.templateId ?: 0)
        putInt(playerCharacter.inventory.necklace?.templateId ?: 0)
        putInt(playerCharacter.inventory.leftRing?.templateId ?: 0)
        putInt(playerCharacter.inventory.rightRing?.templateId ?: 0)
        putInt(playerCharacter.inventory.headgear?.templateId ?: 0)
        putInt(playerCharacter.inventory.oneHanded?.templateId ?: 0)
        putInt(playerCharacter.inventory.shield?.templateId ?: 0)
        putInt(playerCharacter.inventory.gloves?.templateId ?: 0)
        putInt(playerCharacter.inventory.upperBody?.templateId ?: 0)
        putInt(playerCharacter.inventory.lowerBody?.templateId ?: 0)
        putInt(playerCharacter.inventory.boots?.templateId ?: 0)
        putInt(playerCharacter.inventory.underwear?.templateId ?: 0)
        putInt(playerCharacter.inventory.twoHanded?.templateId ?: 0)
        putInt(playerCharacter.inventory.hairAccessory?.templateId
            ?: playerCharacter.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(playerCharacter.inventory.faceAccessory?.templateId ?: 0)

        put(ByteArray(28))
        putInt(playerCharacter.inventory.weapon?.augmentationId ?: 0) //at l2j - right hand item augmentation
        put(ByteArray(24))
        putInt(playerCharacter.inventory.weapon?.augmentationId ?: 0) //at l2j - two-handed item augmentation
        put(ByteArray(8))

        putInt(playerCharacter.stats.pAtk)
        putInt(playerCharacter.stats.atkSpd)
        putInt(playerCharacter.stats.pDef)
        putInt(playerCharacter.stats.evasion)
        putInt(playerCharacter.stats.accuracy)
        putInt(playerCharacter.stats.critRate)
        putInt(playerCharacter.stats.mAtk)
        putInt(playerCharacter.stats.castingSpd)
        putInt(playerCharacter.stats.atkSpd) //TODO ??? Twice?
        putInt(playerCharacter.stats.mDef)

        putInt(playerCharacter.pvpState.ordinal)
        putInt(playerCharacter.karma)

        putInt((playerCharacter.stats.speed / speedMultiplier).toInt())
        putInt((playerCharacter.stats.walkSpeed / speedMultiplier).toInt())
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO Swim run speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO Swim walk speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fl (??) run speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fl (??) walk speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fly run speed
        putInt((playerCharacter.stats.speed / speedMultiplier).toInt()) //TODO fly walk speed
        putDouble(speedMultiplier)
        putDouble(atkSpeedMultiplier)

        //TODO Hitbox should be taken from pet if mounted
        putDouble(playerCharacter.collisionBox.radius)
        putDouble(playerCharacter.collisionBox.height)

        putInt(playerCharacter.hairStyle)
        putInt(playerCharacter.hairColor)
        putInt(playerCharacter.faceType)

        putInt(playerCharacter.accessLevel.ordinal)
        putUTF16String(playerCharacter.title)

        putInt(playerCharacter.clanId)
        putInt(0) //TODO Clan crest id
        putInt(0) //TODO Alliance id
        putInt(0) //TODO Alliance crest id
        putInt(0) //TODO Siege relation

        put(0) //TODO Mount type. 0 - none, 1 - strider, 2 - wyvern

        put(playerCharacter.privateStore?.storeType?.toByte() ?: 0)

        put(0) //TODO Dwarven Craft

        putInt(playerCharacter.pkCount)
        putInt(playerCharacter.pvpCount)

        //TODO Cubics - first goes cubics amount, then their ids
        putShort(0) //cubic amount
        //putShort(cubic.id) cubic ids

        put(0)

        putInt(0) //TODO Abnormal Effects - stunned, rooted, etc in .
        put(0)

        putInt(0)// TODO clan privileges

        putShort(0) //TODO Recommendations Left
        putShort(0) //TODO Recommendations Received
        putInt(0)

        putShort(100) //TODO Inventory limit

        putInt(playerCharacter.characterClass.id)
        putInt(0) // special effects? circles around player... (c)L2J

        putInt(playerCharacter.stats.maxCp)
        putInt(playerCharacter.currentCp)

        put(minOf(16, playerCharacter.inventory.weapon?.enchantLevel ?: 0).toByte()) //TODO if mounted - 0

        put(0) //TODO team circle around feet 1 = Blue, 2 = red

        putInt(0) //TODO getClanCrestLargeId

        put(0) //TODO isNoble

        put(0) //TODO isHero

        put(0) //TODO isFishing
        putInt(0) //TODO Fish X
        putInt(0) //TODO Fish Y
        putInt(0) //TODO Fish Z

        putInt(playerCharacter.nameColor)
        put((playerCharacter.moveType == MoveType.RUN).toByte())

        putInt(0) //TODO PledgeClass(?)

        putInt(0)
        putInt(playerCharacter.titleColor)
        putInt(0)
    }
    
}
