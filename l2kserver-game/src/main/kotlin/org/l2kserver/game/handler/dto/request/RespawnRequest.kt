package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val RESPAWN_REQUEST_PACKET_ID: UByte = 109u

/**
 * Request to respawn dead character
 *
 * @property respawnAt Where should be this character respawned
 */
data class RespawnRequest(val respawnAt: RespawnAt): RequestPacket {
    constructor(data: ByteBuffer): this(RespawnAt.getRespawnPointById(data.getInt()))
}

enum class RespawnAt {
    VILLAGE,
    CLAN_HALL,
    CASTLE,
    SIEGE_HEADQUARTERS,
    FIXED,
    JAIL;

    companion object {
        fun getRespawnPointById(pointId: Int) = when(pointId) {
            0 -> VILLAGE
            1 -> CLAN_HALL
            2 -> CASTLE
            3 -> SIEGE_HEADQUARTERS
            4 -> FIXED

            else -> JAIL
        }
    }
}
