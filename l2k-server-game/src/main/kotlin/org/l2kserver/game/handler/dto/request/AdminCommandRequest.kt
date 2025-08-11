package org.l2kserver.game.handler.dto.request

import java.nio.ByteBuffer
import org.l2kserver.game.extensions.getUTF16String

const val ADMIN_COMMAND_REQUEST_PACKET_ID: UByte = 91u

/**
 * Request to execute admin command
 *
 * @property commandString Command string.
 *
 * @see org.l2kserver.game.model.command.Command
 */
data class AdminCommandRequest(val commandString: String): RequestPacket {

    constructor(data: ByteBuffer): this(
        commandString = data.getUTF16String()
    )

}
