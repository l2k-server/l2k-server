package org.l2kserver.game.handler.dto.response

import org.l2kserver.game.extensions.littleEndianByteArray
import org.l2kserver.game.extensions.putUByte

private const val DELETE_OBJECT_RESPONSE_PACKET_ID: UByte = 18u

/**
 * Notifies about GameObject deletion from game world
 *
 * @property gameObjectId Deleted game object's identifier
 */
data class DeleteObjectResponse(
    val gameObjectId: Int
): ResponsePacket {

    override val data = littleEndianByteArray {
        putUByte(DELETE_OBJECT_RESPONSE_PACKET_ID)
        putInt(gameObjectId)
        putInt(0)
    }

}
