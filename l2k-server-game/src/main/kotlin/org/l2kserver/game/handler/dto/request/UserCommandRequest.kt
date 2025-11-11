package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer

const val USER_COMMAND_REQUEST_PACKET_ID: UByte = 170u

/**
 * Request to execute user command
 *
 * @property command Command to execute
 */
data class UserCommandRequest(
    val command: UserCommand
): RequestPacket {
    constructor(data: ByteBuffer) : this(UserCommand.byId(data.getInt()))
}

enum class UserCommand(private val ids: Set<Int>) {
    LOC(ids = setOf(0)),
    UNSTUCK(ids = setOf(52)),
    MOUNT(ids = setOf(61)),
    DISMOUNT(ids = setOf(62)),
    DELAY(ids = setOf(76)),
    TIME(ids = setOf(77)),
    PARTY_INFO(ids = setOf(81)),
    CLAN_WAR_LIST(ids = setOf(88, 89, 90)), //TODO check if all of them are used
    CHANNEL_DELETE(ids = setOf(93)),
    CHANNEL_LEAVE(ids = setOf(96)),
    CHANNEL_LIST_UPDATE(ids = setOf(97)),
    CLAN_PENALTY(ids = setOf(100)),
    OLYMPIAD_STAT(ids = setOf(109));

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.ids.contains(id) }) { "Invalid command id '$id'" }
    }
}
