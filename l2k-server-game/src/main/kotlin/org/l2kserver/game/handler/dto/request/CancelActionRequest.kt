package org.l2kserver.game.handler.dto.request

const val CANCEL_ACTION_REQUEST_PACKET_ID: UByte = 55u

/**
 * Request to cancel current action
 */
//This packet contains 'unselect' Short value, that seems useless
data object CancelActionRequest: RequestPacket
