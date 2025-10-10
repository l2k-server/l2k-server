package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class LearnCommand: AdminCommand() {

    val skillId by argument("skill ID").int()
    val name by option("-by")
    val level by option("-level").int().default(1)

    companion object : CommandDescription {
        override val manual = "'learn' -Learns skill with provided ID to provided character. " +
                "If you don't provide a character name, skill will be learned by your current character.\n" +
                "Usage: //learn <skillId> -level <skillLevel> -by <characterName>\n" +
                "Example: //learn 1 -level 3 -by Nagibator777\n" +
                "Example to learn skill 1 by current character: //learn 1"
    }

    override fun toString()= "LearnCommand(name=$name, skillId=$skillId, level=$level)"
}
