package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUTF16String

const val CREATE_CHARACTER_REQUEST_PACKET_ID: UByte = 11u

/**
 * Request to delete a character.
 * Contains parameters for character creation set by player - class, race, appearance
 */
data class CreateCharacterRequest(
    val characterName: String,
    val raceId: Int,
    val genderId: Int,
    val classId: Int,
    val int: Int = 0,
    val str: Int = 0,
    val con: Int = 0,
    val men: Int = 0,
    val dex: Int = 0,
    val wit: Int = 0,
    val hairStyle: Int,
    val hairColor: Int,
    val faceType: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(
        characterName = data.getUTF16String(),
        raceId = data.getInt(),
        genderId = data.getInt(),
        classId = data.getInt(),
        int = data.getInt(),
        str = data.getInt(),
        con = data.getInt(),
        men = data.getInt(),
        dex = data.getInt(),
        wit = data.getInt(),
        hairStyle = data.getInt(),
        hairColor = data.getInt(),
        faceType = data.getInt()
    )

}
