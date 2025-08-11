package org.l2kserver.login.handler.dto.response

import org.l2kserver.login.security.CryptUtils

private const val SERVER_LIST_RESPONSE_PACKET_ID: Byte = 0x04

data class ServerListResponse(
    val lastServerId: Byte,
    val servers: List<GameServerInfo>
): ResponsePacket {

    override fun getEncryptedData(blowfishKey: ByteArray): ByteArray {
        val byteBuffer = ResponsePacket.createBuffer()
            .put(ByteArray(ResponsePacket.HEADER_SIZE))// reserve two bytes for header
            .put(SERVER_LIST_RESPONSE_PACKET_ID)
            .put(servers.size.toByte())
            .put(lastServerId)

        servers.forEach {
            byteBuffer
                .put(it.id)
                .put(it.serverIp)
                .putInt(it.port)
                .put(it.ageLimit)
                .put(if(it.isPvp) 1 else 0)
                .putShort(it.currentPlayers)
                .putShort(it.maxPlayers)
                .put(if(it.isOnline) 1 else 0)
                .putInt(it.serverType.index)
                .put(if(it.showBrackets) 1 else 0)
        }

        byteBuffer.putShort(0)
        //TODO characters on servers
            .put(0)



        val newSize = ResponsePacket.HEADER_SIZE + CryptUtils.encrypt(blowfishKey, byteBuffer)

        return byteBuffer
            .position(0)
            .putShort(newSize.toShort())
            .array()
            .sliceArray(0 until newSize)
    }

}

/**
 * Information about GameServer
 *
 * @param id Server id. Maps to server name at client side (for example 1 is Bartz)
 * @param serverIp Server ipV4 address as array of 4 bytes (for example [127, 0, 0, 1])
 * @param port Gameserver port
 * @param ageLimit Server age limit (0, 15, 18)
 * @param isPvp Is this server pvp
 * @param currentPlayers Current server online players amount
 * @param maxPlayers Maximum of players gameserver allows
 * @param isOnline Is this gameserver online
 * @param serverType Server type (Normal, Relax, etc.)
 * @param showBrackets Show square brackets before server name, or not
 */
data class GameServerInfo(
    val id: Byte,
    val serverIp: ByteArray,
    val port: Int,
    val ageLimit: Byte,
    val isPvp: Boolean,
    val currentPlayers: Short,
    val maxPlayers: Short,
    val isOnline: Boolean,
    val serverType: ServerType = ServerType.NORMAL,
    val showBrackets: Boolean // TODO wtf???????
)

enum class ServerType(val index: Int) {
    NORMAL(1),
    RELAX(2),
    PUBLIC_TEST(4),
    NO_LABEL(8),
    CHARACTER_CREATION_RESTRICTED(16),
    EVENT(32),
    FREE(64)
}
