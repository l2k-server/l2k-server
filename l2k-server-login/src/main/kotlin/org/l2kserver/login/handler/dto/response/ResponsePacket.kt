package org.l2kserver.login.handler.dto.response

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface ResponsePacket {

    /**
     * Transforms this packet to byte array and encrypts it
     * @param blowfishKey encryption key
     *
     * @return ready-to-send byte array
     */
    fun getEncryptedData(blowfishKey: ByteArray): ByteArray

    companion object {
        @JvmStatic
        val DEFAULT_BUFFER_SIZE = 64 * 1024

        @JvmStatic
        val HEADER_SIZE = Short.SIZE_BYTES

        @JvmStatic
        fun createBuffer(): ByteBuffer = ByteBuffer
            .wrap(ByteArray(DEFAULT_BUFFER_SIZE))
            .order(ByteOrder.LITTLE_ENDIAN)

    }

}
