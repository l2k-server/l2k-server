package org.l2kserver.game.model.utils

import kotlin.random.Random

/**
 * Performs provided [action] with given [chance] (1 means 100% chance).
 *
 * If [chance] is greater than 1, integer part will mean how many times action will be performed with guarantee.
 * For example, chance 2.5 will provide two guaranteed actions and one with 50% chance
 **/
inline fun <T> withChance(chance: Double, action: () -> T): List<T> {
    val results = mutableListOf<T>()
    var nextActionChance = chance

    while (nextActionChance > 0) {
        if (Random.nextDouble() < nextActionChance) results.add(action())
        nextActionChance--
    }

    return results
}
