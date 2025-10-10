package org.l2kserver.game.model.skill.effect

import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.stats.CombatStatsMultipliers
import org.l2kserver.game.model.stats.TradeAndInventoryStats

data class StatsEffect(
    val bonusCombatStats: CombatStats? = null,
    val combatStatsMultipliers: CombatStatsMultipliers? = null,
    val bonusBasicStats: BasicStats? = null,
    val bonusTradeAndInventoryStats: TradeAndInventoryStats? = null
): Effect
