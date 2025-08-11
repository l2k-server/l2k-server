package org.l2kserver.game.extensions.model.stats

import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats

fun TradeAndInventoryStats.applyBasicStats(basicStats: BasicStats) = this.copy(
    weightLimit = basicStats.con.baseMaxWeight
)
