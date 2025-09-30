package org.l2kserver.game.handler.dto.request

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.int
import org.l2kserver.game.extensions.getUTF16String
import java.nio.ByteBuffer

const val BYPASS_TO_SERVER_REQUEST_PACKET_ID: UByte = 33u

class BypassToServerRequest: RequestPacket, NoOpCliktCommand() {
    val npcId by option("-npc").int()
    val quest by option("-quest").int().optionalValue(0)

    override fun toString() = "BypassToServerRequest(npcId=$npcId, quest=$quest)"
}

fun BypassToServerRequest(data: ByteBuffer): BypassToServerRequest {
    val commandString = data.getUTF16String()
    val commandArgs = commandString.trim().split("\\s+".toRegex())
    val bypassToServerRequest = BypassToServerRequest()

    runCatching { bypassToServerRequest.parse(commandArgs) }.onFailure {
        throw IllegalArgumentException(it.message ?: "Failed to parse bypass command '$commandString'")
    }

    return bypassToServerRequest
}
