package org.l2kserver.game.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.TreeMap

@ConfigurationProperties(prefix = "level")
data class LevelProperties(
    val expLoss: TreeMap<Int, Double>,
    val maxLevel: Int,
    val exp: TreeMap<Int, Long>
) {

    private val expToLevel = TreeMap<Long, Int>(exp.entries.reversed().associate { it.value to it.key })

    init {
        val missedLevels = mutableListOf<Int>()
        for (i: Int in 1..exp.lastKey()) if (exp[i] == null) missedLevels.add(i)
        require(missedLevels.isEmpty()) {
            "[level.exp] No EXP amount defined for levels ${missedLevels.joinToString(",")}"
        }
        require(maxLevel < exp.lastKey()) {
            "[level.maxLevel] $maxLevel must be at least 1 less than the last level in the table! " +
                    "Current maximum available maxLevel is ${exp.lastKey() - 1}"
        }

    }

    fun getExpRange() = expToLevel.firstKey()..exp[maxLevel]!!

    fun getLevelByExp(exp: Long) = requireNotNull(expToLevel.floorEntry(exp)?.value) {
        "Invalid value of exp=$exp. Available values are from ${expToLevel.firstKey()} to ${expToLevel.lastKey() - 1}"
    }

    fun getRequiredExpForLevel(level: Int) = requireNotNull(exp[level]) {
        "Cannot find exp amount for level $level. Max level in exp table is ${exp.lastKey()}"
    }

}
