package org.l2kserver.game.model.command

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.parse

sealed class Command : NoOpCliktCommand() {
    companion object {
        fun parse(commandString: String) = runCatching {
            val splitText = commandString.trim().split("\\s+".toRegex())
            val commandName = splitText[0]
            val commandArgs = if (splitText.size > 1) splitText.subList(1, splitText.size) else emptyList()

            val command = when (commandName) {
                "help" -> HelpCommand
                "teleport", "tp" -> TeleportCommand()
                "enchant" -> EnchantCommand()

                else -> throw IllegalArgumentException("Unknown command $commandName")
            }

            runCatching { command.parse(commandArgs) }.onFailure {
                throw IllegalArgumentException(it.message ?: "Failed to parse command string '$commandString'")
            }

            command
        }
    }
}

sealed interface CommandDescription {
    val manual: String
}
