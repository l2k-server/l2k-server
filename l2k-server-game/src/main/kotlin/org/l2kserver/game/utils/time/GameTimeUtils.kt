package org.l2kserver.game.utils.time

import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/** Launches [action] and then waits for provided amount of [millis] */
suspend inline fun<T> withDelay(millis: Long = 100L, action: suspend () -> T) {
    val actionStartTime = System.currentTimeMillis()
    action()
    val delayMills = millis - (System.currentTimeMillis() - actionStartTime)
    if (delayMills < 0) System.err.println("Action took ${delayMills.absoluteValue} ms longer than time allotted!")
    delay(delayMills)
}
