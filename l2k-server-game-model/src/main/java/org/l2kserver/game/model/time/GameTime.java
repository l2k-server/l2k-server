package org.l2kserver.game.model.time;

public class GameTime {
    private GameTime() {
        throw new UnsupportedOperationException();
    }

    public static final int MILLIS_IN_TICK = 100;
    private static final int MILLIS_IN_GAME_MINUTE = 10_000;
    private static final int MINUTES_IN_FULL_DAY = 1440;
    private static final long SERVER_START_TIME = System.currentTimeMillis();

    /** How many millis is server up */
    public static long runtime() {
        return System.currentTimeMillis() - SERVER_START_TIME;
    }

    /** Game time in 'game minutes'. One 'game minute' is 10 real seconds */
    public static int gameMinutes() {
        return (int)(runtime() / MILLIS_IN_GAME_MINUTE) % MINUTES_IN_FULL_DAY;
    }

}
