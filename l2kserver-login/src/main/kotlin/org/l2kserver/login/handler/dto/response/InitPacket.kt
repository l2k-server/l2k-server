package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.security.CryptUtils

private const val INIT_PACKET_ID: Byte = 0x00
private const val PROTOCOL_REVISION = 0x0000C621

/**
 * Init packet
 *
 * @param publicKey Scrambled RSAPublicKey modulus
 * @param blowfishKey BlowfishKey for further packet encryption
 * @param sessionId Session id
 */
data class InitPacket(
    val publicKey: ByteArray,
    val blowfishKey: ByteArray,
    val sessionId: Int
) {

    fun getEncryptedData(): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
                .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
                .put(INIT_PACKET_ID)
                .putInt(sessionId)
                .putInt(PROTOCOL_REVISION)
                .put(publicKey)
                // unk GG related?
                .putInt(0x29DD954E)
                .putInt(0x77C39CFC)
                .putInt(-0x685249E0)
                .putInt(0x07BDE0F7)
                .put(blowfishKey)
                .put(0x00) //null termination ;)

        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encryptInitPacketData(byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }

}
