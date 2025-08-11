package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.extensions.toInt
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.actor.character.PvpState

private const val RELATION_CHANGED_RESPONSE_PACKET_ID: UByte = 206u

/**
 * This packet contains information about PVP relation of character with [characterId] to the packet addressee
 *
 * @property characterId Character ID
 * @property isEnemy can this character be attacked without forcing
 * @property karma Character's karma points
 * @property pvpState Character's PVP state
 */
data class PvPStatusResponse(
    val characterId: Int,
    val isEnemy: Boolean,
    val karma: Int,
    val pvpState: PvpState
): ResponsePacket {

    constructor(playerCharacter: PlayerCharacter): this(
        characterId = playerCharacter.id,
        isEnemy = (playerCharacter.pvpState != PvpState.NOT_IN_PVP) || (playerCharacter.karma > 0),
        karma = playerCharacter.karma,
        pvpState = playerCharacter.pvpState
    )

    override val data = littleEndianByteArray {
        putUByte(RELATION_CHANGED_RESPONSE_PACKET_ID)
        putInt(characterId)
        putInt(0) //TODO Relation calculation
        putInt(isEnemy.toInt())
        putInt(karma)
        putInt(pvpState.ordinal)
    }

}
