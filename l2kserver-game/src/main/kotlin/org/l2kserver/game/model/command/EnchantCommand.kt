package org.l2kserver.game.model.command

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int

/**
 * Command to enchant [itemToEnchant] of [characterName] by requested [enchantLevel]
 */
class EnchantCommand: Command() {
    val itemToEnchant by argument("item to enchant").enum<ItemToEnchant>()
    val characterName by option("-of")
    val enchantLevel by option("-by").int().required()
        .check("Enchant level must be from 0 to 65535") { it in 0..UShort.MAX_VALUE.toInt() }

    companion object : CommandDescription {
        override val manual = "'enchant' - Enchant equipped item of some character. " +
                "If you don't provide a character name, it will enchant your current character's item.\n" +
                "Usage: //enchant [${ItemToEnchant.entries.joinToString(" | ") { it.name.lowercase() }}] " +
                "-of <characterName> -by <enchantLevel>\n" +
                "Example: //enchant weapon -of Nagibator777 -by 16"
    }

    override fun toString() = "EnchantCommand(itemToEnchant=$itemToEnchant, name=$characterName, enchantLevel=$enchantLevel)"
}

enum class ItemToEnchant {
    UNDERWEAR,
    RIGHT_EARRING,
    LEFT_EARRING,
    NECKLACE,
    RIGHT_RING,
    LEFT_RING,
    HEADGEAR,
    WEAPON,
    SHIELD,
    GLOVES,
    UPPER_BODY,
    LOWER_BODY,
    BOOTS
}
