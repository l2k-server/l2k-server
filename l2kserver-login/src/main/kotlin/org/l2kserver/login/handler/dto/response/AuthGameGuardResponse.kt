package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.security.CryptUtils

private const val SKIP_GG_AUTH_REQUEST_ID: Byte = 0x0b

data class AuthGameGuardResponse(
    val sessionId: Int
) : ResponsePacket {

    override fun getEncryptedData(blowfishKey: ByteArray): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
                .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
                .put(SKIP_GG_AUTH_REQUEST_ID)
                .putInt(sessionId)
                .putInt(0x00)
                .putInt(0x00)
                .putInt(0x00)
                .putInt(0x00)

        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encrypt(blowfishKey, byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }

}
