package org.l2kserver.game.data.item.scroll.enchant

import org.l2kserver.game.model.item.EnchantScroll
import org.l2kserver.game.model.item.Grade

data object ScrollEnchantWeaponS: EnchantScroll() {
    override val id = 959
    override val name = "Scroll: Enchant Weapon (Grade S)"
    override val grade = Grade.S
    override val target = Target.WEAPON
    override val isBlessed = false
    override val weight = 120
    override val price = 5000000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = false
}

data object BlessedScrollEnchantWeaponS: EnchantScroll() {
    override val id = 6577
    override val name = "Blessed Scroll: Enchant Weapon (Grade S)"
    override val grade = Grade.S
    override val target = Target.WEAPON
    override val isBlessed = true
    override val weight = 120
    override val price = 30000000
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val isStackable = false
}
