package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

enum class StatToRestore {
    CP, HP, MP
}
class RestoreCommand: AdminCommand() {
    val statsToRestore: List<StatToRestore>? by argument()
        .enum<StatToRestore>()
        .multiple()
        .optional()

    val name by option("-of")

    companion object : CommandDescription {
        override val manual = "'restore' - restore all the cp, hp or mp of some character" +
                "If you don't provide a character name, it will restore your current character.\n" +
                "Usage: //restore [${StatToRestore.entries.joinToString(" | ") { it.name.lowercase() }}]" +
                " -of <characterName>\n" +
                "Example: //restore cp,mp -of Nagibator777"
    }
}
