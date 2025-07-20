package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.security.CryptUtils

private const val SELECT_GAMESERVER_SUCCESS_RESPONSE_PACKET_ID: Byte = 0x07

data class SelectGameserverSuccessResponse(
    val gameSessionKey1: Int,
    val gameSessionKey2: Int
): ResponsePacket {

    override fun getEncryptedData(blowfishKey: ByteArray): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
            .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
            .put(SELECT_GAMESERVER_SUCCESS_RESPONSE_PACKET_ID)
            .putInt(gameSessionKey1)
            .putInt(gameSessionKey2)

        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encrypt(blowfishKey, byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }

}
