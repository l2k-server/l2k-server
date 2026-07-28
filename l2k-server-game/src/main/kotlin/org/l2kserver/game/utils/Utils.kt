package org.l2kserver.game.utils

import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.streams.asSequence

/**
 * Performs provided [action] with given [chance] (1 means 100% chance).
 *
 * If [chance] is greater than 1, integer part will mean how many times action will be performed for sure.
 * For example, [chance] 2.5 guarantees 2 actions and one with 50% chance
 *
 * @return List of action results
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

/** Launches [action] and then waits for provided amount of [millis] */
suspend inline fun<T> withDelay(millis: Long = 100L, action: suspend () -> T) {
    val actionStartTime = System.currentTimeMillis()
    action()
    if (millis <= 0L) return

    val delayMills = millis - (System.currentTimeMillis() - actionStartTime)
    if (delayMills < 0) {
        val actionCaller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk { frames ->
            frames.asSequence().first().toStackTraceElement().toString()
        }
        System.err.println("Action $actionCaller took ${delayMills.absoluteValue} ms longer than allotted $millis ms!")
    }
    delay(delayMills)
}
