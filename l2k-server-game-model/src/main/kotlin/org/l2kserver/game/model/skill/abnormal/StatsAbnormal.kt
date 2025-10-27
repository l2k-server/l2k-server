package org.l2kserver.game.model.skill.abnormal

import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.CombatStatsMultipliers
import org.l2kserver.game.model.stats.TradeAndInventoryStats

data class StatsAbnormal(
    val bonusCombatStats: CombatStats? = null,
    val combatStatsMultipliers: CombatStatsMultipliers? = null,
    val bonusBasicStats: BasicStats? = null,
    val bonusTradeAndInventoryStats: TradeAndInventoryStats? = null
): Abnormal

fun Abnormals.multiplyPAtk(value: Double) = this.add(
    StatsAbnormal(combatStatsMultipliers = CombatStatsMultipliers(pAtk = value))
)

fun Abnormals.multiplyCastingSpd(value: Double) = this.add(
    StatsAbnormal(combatStatsMultipliers = CombatStatsMultipliers(castingSpd = value))
)
