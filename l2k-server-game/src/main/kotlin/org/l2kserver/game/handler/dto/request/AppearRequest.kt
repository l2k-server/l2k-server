package org.l2kserver.game.handler.dto.request

const val APPEAR_REQUEST_PACKET_ID: UByte = 48u

/** Requests characterInfo after appearing (f.e. after teleporting) */
data object AppearRequest: RequestPacket
