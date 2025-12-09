package org.l2kserver.game.data.item.jewelry

import org.l2kserver.game.model.item.template.Grade
import org.l2kserver.game.model.item.template.JewelryTemplate
import org.l2kserver.game.model.item.template.JewelryType
import org.l2kserver.game.model.stats.CombatStats

object EarringOfStrength: JewelryTemplate() {
    override val id = 114
    override val name = "Earring of Strength"
    override val grade = Grade.NO_GRADE
    override val weight = 150
    override val price = 3510
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = JewelryType.EARRING
    override val stats = CombatStats(mDef = 16)
    override val crystalCount = 0
}

object EarringOfWisdom: JewelryTemplate() {
    override val id = 115
    override val name = "Earring of Wisdom"
    override val grade = Grade.NO_GRADE
    override val weight = 150
    override val price = 3510
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = JewelryType.EARRING
    override val stats = CombatStats(mDef = 16)
    override val crystalCount = 0
}

object RingOfKnowledge: JewelryTemplate() {
    override val id = 875
    override val name = "Ring of Knowledge"
    override val grade = Grade.NO_GRADE
    override val weight = 150
    override val price = 540
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = JewelryType.RING
    override val stats = CombatStats(mDef = 9)
    override val crystalCount = 0
}

object RingOfAnguish: JewelryTemplate() {
    override val id = 876
    override val name = "Ring of Anguish"
    override val grade = Grade.NO_GRADE
    override val weight = 150
    override val price = 2340
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = JewelryType.RING
    override val stats = CombatStats(mDef = 11)
    override val crystalCount = 0
}

object NecklaceOfCourage: JewelryTemplate() {
    override val id = 1506
    override val name = "Necklace of Courage"
    override val grade = Grade.NO_GRADE
    override val weight = 150
    override val price = 66
    override val isSellable = true
    override val isDroppable = true
    override val isDestroyable = true
    override val isExchangeable = true
    override val type = JewelryType.NECKLACE
    override val stats = CombatStats(mDef = 15)
    override val crystalCount = 0
}
