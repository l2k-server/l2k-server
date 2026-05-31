package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toByte
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.actor.MoveType
import kotlin.math.roundToInt

private const val CHARACTER_INFO_RESPONSE_PACKET_ID: UByte = 4u

private const val MAX_ENCHANTMENT_EFFECT_VALUE = 16

/** Full character info, which is sent to a player who plays this character */
data class FullCharacterResponse(
    val character: PlayerCharacterInstanceImpl
): ResponsePacket {

    @Suppress("LongMethod")
    override val data = littleEndianByteArray {
        // Speed multiplier. Client needs it for proper animations
        val speedMultiplier = character.stats.speed.toDouble() / character.characterClass.baseSpeed
        // Attack speed multiplier. Client needs it for proper animations
        val atkSpeedMultiplier = character.stats.atkSpd.toDouble() / character.characterClass.baseAtkSpd

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

        putInt(character.level)
        putLong(character.exp)

        putInt(character.basicStats.str.value)
        putInt(character.basicStats.dex.value)
        putInt(character.basicStats.con.value)
        putInt(character.basicStats.int.value)
        putInt(character.basicStats.wit.value)
        putInt(character.basicStats.men.value)

        putInt(character.stats.maxHp.roundToInt())
        putInt(character.currentHp)
        putInt(character.stats.maxMp.roundToInt())
        putInt(character.currentMp)

        putInt(character.sp)

        putInt(character.inventory.weight)
        putInt(character.tradeAndInventoryStats.weightLimit)

        putInt(40)

        putInt(character.inventory.twoSlotsAccessory?.id ?: 0)
        putInt(character.inventory.leftEarring?.id ?: 0)
        putInt(character.inventory.rightEarring?.id ?: 0)
        putInt(character.inventory.necklace?.id ?: 0)
        putInt(character.inventory.leftRing?.id ?: 0)
        putInt(character.inventory.rightRing?.id ?: 0)
        putInt(character.inventory.headgear?.id ?: 0)
        putInt(character.inventory.oneHanded?.id ?: 0)
        putInt(character.inventory.shield?.id ?: 0)
        putInt(character.inventory.gloves?.id ?: 0)
        putInt(character.inventory.upperBody?.id ?: 0)
        putInt(character.inventory.lowerBody?.id ?: 0)
        putInt(character.inventory.boots?.id ?: 0)
        putInt(character.inventory.underwear?.id ?: 0)
        putInt(character.inventory.twoHanded?.id ?: 0)
        putInt(character.inventory.hairAccessory?.id
            ?: character.inventory.twoSlotsAccessory?.id ?: 0)
        putInt(character.inventory.faceAccessory?.id ?: 0)

        putInt(character.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(character.inventory.leftEarring?.templateId ?: 0)
        putInt(character.inventory.rightEarring?.templateId ?: 0)
        putInt(character.inventory.necklace?.templateId ?: 0)
        putInt(character.inventory.leftRing?.templateId ?: 0)
        putInt(character.inventory.rightRing?.templateId ?: 0)
        putInt(character.inventory.headgear?.templateId ?: 0)
        putInt(character.inventory.oneHanded?.templateId ?: 0)
        putInt(character.inventory.shield?.templateId ?: 0)
        putInt(character.inventory.gloves?.templateId ?: 0)
        putInt(character.inventory.upperBody?.templateId ?: 0)
        putInt(character.inventory.lowerBody?.templateId ?: 0)
        putInt(character.inventory.boots?.templateId ?: 0)
        putInt(character.inventory.underwear?.templateId ?: 0)
        putInt(character.inventory.twoHanded?.templateId ?: 0)
        putInt(character.inventory.hairAccessory?.templateId
            ?: character.inventory.twoSlotsAccessory?.templateId ?: 0)
        putInt(character.inventory.faceAccessory?.templateId ?: 0)

        put(ByteArray(28))
        putInt(character.inventory.weapon?.augmentationId ?: 0) //at l2j - right hand item augmentation
        put(ByteArray(24))
        putInt(character.inventory.weapon?.augmentationId ?: 0) //at l2j - two-handed item augmentation
        put(ByteArray(8))

        putInt(character.stats.pAtk)
        putInt(character.stats.atkSpd)
        putInt(character.stats.pDef)
        putInt(character.stats.evasion)
        putInt(character.stats.accuracy)
        putInt(character.stats.critRate)
        putInt(character.stats.mAtk)
        putInt(character.stats.castingSpd)
        putInt(character.stats.atkSpd) //TODO ??? Twice?
        putInt(character.stats.mDef)

        putInt(character.pvpState.ordinal)
        putInt(character.karma)

        putInt((character.stats.speed / speedMultiplier).toInt())
        putInt((character.stats.walkSpeed / speedMultiplier).toInt())
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO Swim run speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO Swim walk speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fl (??) run speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fl (??) walk speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fly run speed
        putInt((character.stats.speed / speedMultiplier).toInt()) //TODO fly walk speed
        putDouble(speedMultiplier)
        putDouble(atkSpeedMultiplier)

        //TODO Hitbox should be taken from pet if mounted
        putDouble(character.collisionBox.radius)
        putDouble(character.collisionBox.height)

        putInt(character.hairStyle)
        putInt(character.hairColor)
        putInt(character.faceType)

        putInt(character.accessLevel.ordinal)
        putUTF16String(character.title)

        putInt(character.clanId)
        putInt(0) //TODO Clan crest id
        putInt(0) //TODO Alliance id
        putInt(0) //TODO Alliance crest id
        putInt(0) //TODO Siege relation

        put(0) //TODO Mount type. 0 - none, 1 - strider, 2 - wyvern

        put(character.privateStore?.storeType?.toByte() ?: 0)

        put(0) //TODO Dwarven Craft

        putInt(character.pkCount)
        putInt(character.pvpCount)

        //TODO Cubics - first goes cubics amount, then their ids
        putShort(0) //cubic amount
        //putShort(cubic.id) cubic ids

        put(0)

        putInt(character.temporalEffects.visible)
        put(0)

        putInt(0)// TODO clan privileges

        putShort(0) //TODO Recommendations Left
        putShort(0) //TODO Recommendations Received
        putInt(0)

        putShort(100) //TODO Inventory limit

        putInt(character.characterClass.id)
        putInt(0) // special effects? circles around player... (c)L2J

        putInt(character.stats.maxCp.roundToInt())
        putInt(character.currentCp)

        put(minOf(MAX_ENCHANTMENT_EFFECT_VALUE, character.inventory.weapon?.enchantLevel ?: 0)
            .toByte()) //TODO if mounted - 0

        put(0) //TODO team circle around feet 1 = Blue, 2 = red

        putInt(0) //TODO getClanCrestLargeId

        put(0) //TODO isNoble

        put(0) //TODO isHero

        put(0) //TODO isFishing
        putInt(0) //TODO Fish X
        putInt(0) //TODO Fish Y
        putInt(0) //TODO Fish Z

        putInt(character.nameColor)
        put((character.moveType == MoveType.RUN).toByte())

        putInt(0) //TODO PledgeClass(?)

        putInt(0)
        putInt(character.titleColor)
        putInt(0)
    }
    
}
