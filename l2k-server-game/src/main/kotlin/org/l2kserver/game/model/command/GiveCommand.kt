package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.l2kserver.game.model.item.template.ItemTemplateRegistry

class GiveCommand: AdminCommand() {

    val templateId by argument("item").int()
        .check({ "No item found in game data by id = '$it'" }) { ItemTemplateRegistry.existsById(it) }

    val name by option("-to")

    val amount by option("-amount").int().default(1)
        .check("Item amount must be greater than 0") { it > 0 }
    
    val enchantLevel by option("-enchantedBy").int().default(0)
        .check("Enchant level must be from 0 to 65535") { it in 0..UShort.MAX_VALUE.toInt() }

    companion object : CommandDescription {
        override val manual =
            "'give' - Gives some amount (default 1) of item with provided ID to provided character. " +
                "If you don't provide a character name, it will give item to your current character.\n" +
                "Usage: //give <characterName> -item <itemTemplateId> " +
                    "-amount <itemAmount> -enchantedBy <enchantLevel>\n" +
                "Example: //give 6372 -to Nagibator777 -amount 10\n" +
                "Example to give current character 1 item: //give 6372"
    }

    override fun toString() = "GiveCommand(name=$name, templateId=$templateId, amount=$amount)"
}
