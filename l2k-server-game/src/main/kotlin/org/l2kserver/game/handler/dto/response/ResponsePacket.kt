package org.l2kserver.game.handler.dto.response

sealed interface ResponsePacket {
    val data: ByteArray

    companion object {
        @JvmStatic
        val HEADER_SIZE = Short.SIZE_BYTES
    }

}
