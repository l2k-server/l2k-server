package org.l2kserver.game.model.time

object GameTime {
    const val MILLIS_IN_TICK = 100L

    private const val MILLIS_IN_GAME_MINUTE = 10_000
    private const val MINUTES_IN_FULL_DAY = 1440
    private val serverStartTime = System.currentTimeMillis()

    /** How many millis is server up */
    val runtime: Long get() = System.currentTimeMillis() - serverStartTime

    /** Game time in 'game minutes'. One 'game minute' is 10 seconds */
    val gameMinutes: Int get() = ((runtime / MILLIS_IN_GAME_MINUTE) % MINUTES_IN_FULL_DAY).toInt()
}
