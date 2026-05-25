package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

class KillCommand: AdminCommand() {

    val victimName by argument("Name of character to kill").optional()

    companion object: CommandDescription {
        override val manual = "'kill' -Kills provided character or NPC. " +
                "If you don't provide a character name, this command will kill your target\n" +
                "Usage: //kill <characterName>\n" +
                "Example: //kill Nagibator777\n"
    }
}
