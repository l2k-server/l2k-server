package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUTF16String
import org.l2kserver.game.extensions.putUByte
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.session.AuthorizationKey
import org.l2kserver.game.utils.GameTimeUtils

private const val SELECT_CHARACTER_RESPONSE_PACKET_ID: UByte = 21u
data class SelectCharacterResponse(
    val authorizationKey: AuthorizationKey,
    val selectedPlayerCharacter: PlayerCharacter
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(SELECT_CHARACTER_RESPONSE_PACKET_ID)

        putUTF16String(selectedPlayerCharacter.name)
        putInt(selectedPlayerCharacter.id)
        putUTF16String(selectedPlayerCharacter.title)
        putInt(authorizationKey.gameSessionKey1)
        putInt(selectedPlayerCharacter.clanId)
        putInt(0)

        putInt(selectedPlayerCharacter.gender.ordinal)
        putInt(selectedPlayerCharacter.race.ordinal)
        putInt(selectedPlayerCharacter.characterClass.name.id)

        putInt(1)

        putInt(selectedPlayerCharacter.position.x)
        putInt(selectedPlayerCharacter.position.y)
        putInt(selectedPlayerCharacter.position.z)

        putDouble(selectedPlayerCharacter.currentHp.toDouble())
        putDouble(selectedPlayerCharacter.currentMp.toDouble())

        putInt(selectedPlayerCharacter.sp)
        putLong(selectedPlayerCharacter.exp)
        putInt(selectedPlayerCharacter.level)
        putInt(selectedPlayerCharacter.karma)

        putInt(0)

        putInt(selectedPlayerCharacter.basicStats.int.value)
        putInt(selectedPlayerCharacter.basicStats.str.value)
        putInt(selectedPlayerCharacter.basicStats.con.value)
        putInt(selectedPlayerCharacter.basicStats.men.value)
        putInt(selectedPlayerCharacter.basicStats.dex.value)
        putInt(selectedPlayerCharacter.basicStats.wit.value)

        repeat(32) { putInt(0)}

        putInt(GameTimeUtils.gameTime.toInt())

        repeat(14) { putInt(0)}
    }
}
