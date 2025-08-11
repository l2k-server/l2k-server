package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val DELETE_CHARACTER_REQUEST_PACKET_ID: UByte = 12u

/**
 * Request to delete a character
 *
 * @property characterSlot character index in player's account characters list
 */
data class DeleteCharacterRequest(
    val characterSlot: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(
        characterSlot = data.getInt()
    )

}
