package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val EXCHANGE_REQUEST_PACKET_ID: UByte = 21u

/**
 * Request to start exchanging with other character
 *
 * @property targetId Identifier of actor to exchange with
 */
data class ExchangeRequest(val targetId: Int): RequestPacket {
    constructor(data: ByteBuffer): this(targetId = data.getInt())
}
