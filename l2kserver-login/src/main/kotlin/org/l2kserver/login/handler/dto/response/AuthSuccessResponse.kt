package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.security.CryptUtils

private const val AUTH_SUCCESS_RESPONSE_PACKET_ID: Byte = 0x03
data class AuthSuccessResponse(
    val loginSessionKey1: Int,
    val loginSessionKey2: Int,
): ResponsePacket {

    override fun getEncryptedData(blowfishKey: ByteArray): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
                .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
                .put(AUTH_SUCCESS_RESPONSE_PACKET_ID)
                .putInt(loginSessionKey1)
                .putInt(loginSessionKey2)
                .put(0x00)
                .put(0x00)
                .putInt(0x000003ea)
                .put(ByteArray(19))

        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encrypt(blowfishKey, byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }
}
