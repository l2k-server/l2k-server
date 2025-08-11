package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val DELETE_SHORTCUT_REQUEST_PACKET_ID: UByte = 53u

/**
 * Request to delete shortcut
 *
 * @property index Index of shortcut to delete
 */
data class DeleteShortcutRequest(
    val index: Int
): RequestPacket {

    constructor(data: ByteBuffer): this(index = data.getInt())

}
