package org.l2kserver.game.model.actor.enumeration

import com.fasterxml.jackson.annotation.JsonProperty

enum class CharacterRace {
    @JsonProperty("Human") HUMAN,
    @JsonProperty("Elf") ELF,
    @JsonProperty("Dark Elf") DARK_ELF,
    @JsonProperty("Orc") ORC,
    @JsonProperty("Dwarf") DWARF
}
