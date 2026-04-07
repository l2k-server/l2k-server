package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.character.PlayerCharacterInstance

private const val CANCEL_TARGET_RESPONSE_PACKET_ID: UByte = 42u

/** Notifies client that character has canceled target */
data class CancelTargetResponse(
    val character: PlayerCharacterInstance,
): ResponsePacket {

    override val data = littleEndianByteArray {
        val characterId = character.id
        val characterPosition = character.position

        putUByte(CANCEL_TARGET_RESPONSE_PACKET_ID)
        putInt(characterId)
        putInt(characterPosition.x)
        putInt(characterPosition.y)
        putInt(characterPosition.z)
    }

}
