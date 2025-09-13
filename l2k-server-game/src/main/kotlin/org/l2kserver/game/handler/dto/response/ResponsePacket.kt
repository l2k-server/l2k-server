package org.l2kserver.game.handler.dto.response

const val EXTENDED_RESPONSE_PACKET_ID: UByte = 254u

sealed interface ResponsePacket {
    val data: ByteArray

    companion object {
        @JvmStatic
        val HEADER_SIZE = Short.SIZE_BYTES
    }

}
