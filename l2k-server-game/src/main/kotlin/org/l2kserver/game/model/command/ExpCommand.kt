package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class ExpCommand: AdminCommand() {
    val amount by argument("Name of character to give him exp").int()

    val characterName by option("-to")

    companion object: CommandDescription {
        override val manual = "'ext' - Gives exp to provided character " +
                "If you don't provide a character name, this command will give exp to your current target.\n" +
                "If you don't have a target, or it cannot get EXP, EXP will be given to your current character.\n" +
                "Usage: //exp <amount> -to <characterName>\n" +
                "Example: //exp 1000 -to Nagibator777\n"
    }
}
