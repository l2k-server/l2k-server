package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.l2kserver.game.model.position.Position

/**
 * Command to teleport
 *
 * @property name Name of character that should be teleported
 * @property position Teleporting target position
 */
class TeleportCommand: Command() {
    val name by argument("character name").optional()
    val position by option("-to").convert { it.toPosition() }.required()

    override fun toString()= "TeleportCommand(name='$name' position=$position)"

    companion object: CommandDescription {
        override val manual = "'teleport' - Teleport character to some position. " +
                "If you don't provide a character name, it will teleport your current character.\n" +
                "Usage: //[teleport | tp] <characterName> -to <x,y,z>\n" +
                "Example: //tp Nagibator777 -to -83990,243336,-3700"
    }
}

private fun String.toPosition(): Position {
    require(this.matches("-?\\d+,-?\\d+,-?\\d+".toRegex())) {"'$this' cannot be parsed to valid Position"}
    return with(this.split(",")) { Position(this[0].toInt(), this[1].toInt(), this[2].toInt()) }
}
