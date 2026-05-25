package org.l2kserver.game.utils

import org.l2kserver.game.extensions.logger
import kotlin.math.roundToLong

class ExpLossCalculator(expLoss: Map<Int, Double>) {

    private val log = logger()
    private val expLossMap = expLoss.toSortedMap()

    init {
        if (expLossMap.isEmpty() || expLossMap.values.all { it == 0.0 })
            log.info("Experience loss after death disabled")
        else log.info("Experience loss after death - {}", expLossMap.toList()
            .joinToString("; ") {(level, loss) -> "Level $level+: $loss%" }
        )
    }

    /** Returns exp loss by character level */
    operator fun get(characterLevel: Int): Long {
        var percentage = 0.0

        for ((level: Int, loss: Double) in expLossMap) {
            if (characterLevel < level) break

            percentage = loss
        }

        val minExpForLevel = LevelUtils.getRequiredExpForLevel(characterLevel)
        val minExpForNextLevel = LevelUtils.getRequiredExpForLevel(characterLevel + 1)
        val totalExpAmountAtLevel = minExpForNextLevel - minExpForLevel
        val expLoss = (totalExpAmountAtLevel * percentage / 100).roundToLong()

        log.debug("Calculated exp loss is {} ({}% of max EXP for level {})", expLoss, percentage, characterLevel)
        return expLoss
    }

}
