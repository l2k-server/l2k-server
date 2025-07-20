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
    LOC(setOf(0)),
    UNSTUCK(setOf(52)),
    MOUNT(setOf(61)),
    DISMOUNT(setOf(62)),
    TIME(setOf(77)),
    PARTY_INFO(setOf(81)),
    CLAN_WAR_LIST(setOf(88, 89, 90)), //TODO check if all of them are used
    CHANNEL_DELETE(setOf(93)),
    CHANNEL_LEAVE(setOf(96)),
    CHANNEL_LIST_UPDATE(setOf(97)),
    CLAN_PENALTY(setOf(100)),
    OLYMPIAD_STAT(setOf(109));

    companion object {
        fun byId(id: Int) = requireNotNull(entries.find { it.ids.contains(id) }) { "Invalid command id '$id'" }
    }
}
