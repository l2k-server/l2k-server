package org.l2kserver.game.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.TreeMap

@ConfigurationProperties(prefix = "enchantment")
data class EnchantProperties(
    val weaponChance: TreeMap<Int, Double>,
    val magicWeaponChance: TreeMap<Int, Double>,
    val armorChance: TreeMap<Int, Double>
)
