package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.model.actor.PlayerCharacter
import java.time.Duration
import java.time.LocalDateTime

private const val CHARACTERS_INFO_RESPONSE_PACKET_ID: UByte = 19u

/**
 * List of characters on player's account
 *
 * @param gameSessionKey1 gameSessionKey1
 * @param accountName Player's account name
 * @param playerCharacters List of characters
 */
data class CharacterListResponse(
    val gameSessionKey1: Int,
    val accountName: String,
    val playerCharacters: List<PlayerCharacter>
) : ResponsePacket {

    override val data = littleEndianByteArray {
       put(CHARACTERS_INFO_RESPONSE_PACKET_ID.toByte())
       putInt(playerCharacters.size)

        val lastActiveCharacterId = playerCharacters
            .filter { it.deletionDate == null }
            .maxByOrNull { it.lastAccess }?.id ?: 0

        playerCharacters.forEach {
            putUTF16String(it.name)
            putInt(it.id)
            putUTF16String(accountName)
            putInt(gameSessionKey1)
            putInt(it.clanId)
            putInt(0)

            putInt(it.gender.ordinal)
            putInt(it.race.ordinal)
            putInt(it.characterClass.baseClassId)
            putInt(1)

            putInt(0) // x
            putInt(0) // y
            putInt(0) // z

            putDouble(it.currentHp.toDouble())
            putDouble(it.currentMp.toDouble())

            putInt(it.sp)
            putLong(it.exp)
            putInt(it.level)
            putInt(it.karma)

            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(0)

            putInt(it.paperDoll.twoSlotsAccessory?.id ?: 0)
            putInt(it.paperDoll.rightEarring?.id ?: 0)
            putInt(it.paperDoll.leftEarring?.id ?: 0)
            putInt(it.paperDoll.necklace?.id ?: 0)
            putInt(it.paperDoll.rightRing?.id ?: 0)
            putInt(it.paperDoll.leftRing?.id ?: 0)
            putInt(it.paperDoll.headgear?.id ?: 0)
            putInt(it.paperDoll.oneHanded?.id ?: 0)
            putInt(it.paperDoll.shield?.id ?: 0)
            putInt(it.paperDoll.gloves?.id ?: 0)
            putInt(it.paperDoll.upperBody?.id ?: 0)
            putInt(it.paperDoll.lowerBody?.id ?: 0)
            putInt(it.paperDoll.boots?.id ?: 0)
            putInt(it.paperDoll.underwear?.id ?: 0)
            putInt(it.paperDoll.twoHanded?.id ?: 0)
            putInt(it.paperDoll.faceAccessory?.id ?: it.paperDoll.twoSlotsAccessory?.id ?: 0)
            putInt(it.paperDoll.hairAccessory?.id ?: it.paperDoll.twoSlotsAccessory?.id ?: 0)

            putInt(it.paperDoll.twoSlotsAccessory?.templateId ?: 0)
            putInt(it.paperDoll.rightEarring?.templateId ?: 0)
            putInt(it.paperDoll.leftEarring?.templateId ?: 0)
            putInt(it.paperDoll.necklace?.templateId ?: 0)
            putInt(it.paperDoll.rightRing?.templateId ?: 0)
            putInt(it.paperDoll.leftRing?.templateId ?: 0)
            putInt(it.paperDoll.headgear?.templateId ?: 0)
            putInt(it.paperDoll.oneHanded?.templateId ?: 0)
            putInt(it.paperDoll.shield?.templateId ?: 0)
            putInt(it.paperDoll.gloves?.templateId ?: 0)
            putInt(it.paperDoll.upperBody?.templateId ?: 0)
            putInt(it.paperDoll.lowerBody?.templateId ?: 0)
            putInt(it.paperDoll.boots?.templateId ?: 0)
            putInt(it.paperDoll.underwear?.templateId ?: 0)
            putInt(it.paperDoll.twoHanded?.templateId ?: 0)
            putInt(it.paperDoll.faceAccessory?.templateId ?: 0)
            putInt(it.paperDoll.hairAccessory?.templateId ?: 0)

            putInt(it.hairStyle)
            putInt(it.hairColor)
            putInt(it.faceType)

            putDouble(it.stats.maxHp.toDouble())
            putDouble(it.stats.maxMp.toDouble())

            putInt(
                if (it.deletionDate == null) 0
                else Duration.between(LocalDateTime.now(), it.deletionDate).toSeconds().toInt()
            )

            putInt(it.characterClass.id)
            putInt(if (it.id == lastActiveCharacterId) 1 else 0)
            put(minOf(16, it.paperDoll.getWeapon()?.enchantLevel ?: 0).toByte())
            putInt(it.paperDoll.getWeapon()?.augmentationId ?: 0)
        }
    }

}
