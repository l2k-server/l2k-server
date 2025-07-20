package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.handler.dto.response.enums.FailReason
import org.l2kserver.login.security.CryptUtils

private const val AUTH_FAILED_RESPONSE_PACKET_ID: Byte = 0x01
data class AuthFailedResponse(
    val reason: FailReason
) : ResponsePacket {

    override fun getEncryptedData(blowfishKey: ByteArray): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
                .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
                .put(AUTH_FAILED_RESPONSE_PACKET_ID)
                .put(reason.code)

        //TODO Refactor - duplicate code
        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encrypt(blowfishKey, byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }

}
