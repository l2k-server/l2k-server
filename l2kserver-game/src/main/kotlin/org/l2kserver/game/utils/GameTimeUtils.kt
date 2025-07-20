package org.l2kserver.game.utils

object GameTimeUtils {

    const val TICKS_IN_SECOND = 10L
    const val MILLIS_IN_TICK = 100L

    private val serverStartTime = System.currentTimeMillis()

    /**
     * How many millis is server up
     */
    val runtime: Long get() = System.currentTimeMillis() - serverStartTime

    /**
     * How many ticks is server up
     */
    val gameTicks: Long get() = runtime / MILLIS_IN_TICK

    val gameTime: Long get() = gameTicks / (TICKS_IN_SECOND * 10)

}
