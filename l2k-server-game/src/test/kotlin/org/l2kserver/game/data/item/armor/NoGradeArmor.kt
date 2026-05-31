package org.l2kserver.game.data.item.armor

import org.l2kserver.game.model.item.Armor
import org.l2kserver.game.model.item.ArmorType
import org.l2kserver.game.model.item.Grade
import org.l2kserver.game.model.stats.CombatStats

data object LeatherShield: Armor() {
    override val id = 18
    override val name = "Leather Shield"
    override val grade = Grade.NO_GRADE
    override val weight = 1430
    override val price = 39
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = ArmorType.SHIELD
    override val stats = CombatStats(shieldDef = 47, shieldDefRate = 20, evasion = -8)
    override val crystalCount = 0
}

data object SquiresShirt: Armor() {
    override val id = 1146
    override val name = "Squire's Shirt"
    override val grade = Grade.NO_GRADE
    override val weight = 3301
    override val price = 26
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = ArmorType.UPPER_BODY_LIGHT
    override val stats = CombatStats(pDef = 33)
    override val crystalCount = 0
}

data object SquiresPants: Armor() {
    override val id = 1147
    override val name = "Squire's Pants"
    override val grade = Grade.NO_GRADE
    override val weight = 1750
    override val price = 6
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = ArmorType.LOWER_BODY_LIGHT
    override val stats = CombatStats(pDef = 20)
    override val crystalCount = 0
}

data object ApprenticeTunic: Armor() {
    override val id = 425
    override val name = "Apprentice's Tunic"
    override val grade = Grade.NO_GRADE
    override val weight = 2150
    override val price = 26
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = ArmorType.UPPER_BODY_ROBE
    override val stats = CombatStats(pDef = 17)
    override val fixedBonusStats = CombatStats(maxMp = 19.0)
    override val crystalCount = 0
}

data object ApprenticesStockings: Armor() {
    override val id = 461
    override val name = "Apprentice's Tunic"
    override val grade = Grade.NO_GRADE
    override val weight = 1100
    override val price = 6
    override val isSellable = false
    override val isDroppable = false
    override val isDestroyable = true
    override val isExchangeable = false
    override val type = ArmorType.LOWER_BODY_ROBE
    override val stats = CombatStats(pDef = 10)
    override val fixedBonusStats = CombatStats(maxMp = 10.0)
    override val crystalCount = 0
}
