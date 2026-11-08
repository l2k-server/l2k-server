package org.l2kserver.game.model.actor.position;

import org.jetbrains.annotations.NotNull;

/** Data class that represents object position in the game world */
public record Position(int x, int y, int z) {

    public static final int GEO_CELL_SIZE   = 16;
    public static final int GEO_REGION_SIZE = 32768;
    public static final int MAP_MIN_X = -131072;
    public static final int MAP_MIN_Y = -262144;
    public static final int MAP_MAX_X =  229376;
    public static final int MAP_MAX_Y =  262144;
    public static final int MAP_MIN_Z = -16384;
    public static final int MAP_MAX_Z =  16383;

    public Position {
        x = Math.clamp(x, MAP_MIN_X + 1, MAP_MAX_X - 1);
        y = Math.clamp(y, MAP_MIN_Y + 1, MAP_MAX_Y - 1);
        z = Math.clamp(z, MAP_MIN_Z + 1, MAP_MAX_Z - 1);
    }

    public Position() {
        this(0, 0, 0);
    }

    /** Returns the X index of the geodata tile where this position is located */
    public int regionXId() {
        return (x >> 0xF) + 20;
    }

    /** Returns the Y index of the geodata tile where this position is located */
    public int regionYId() {
        return (y >> 0xF) + 18;
    }

    /** X coordinate INSIDE of region (from 0 to {@link #GEO_REGION_SIZE}) */
    public int inRegionX() {
        return x & (GEO_REGION_SIZE - 1);
    }

    /** Y coordinate INSIDE of region (from 0 to {@link #GEO_REGION_SIZE}) */
    public int inRegionY() {
        return y & (GEO_REGION_SIZE - 1);
    }

    public int deltaX(Position other) {
        return other.x - this.x;
    }

    public int deltaY(Position other) {
        return other.y - this.y;
    }

    public int deltaZ(Position other) {
        return other.z - this.z;
    }

    /**
     * Calculate distance between the positions (3D)
     */
    public int distanceTo(Position other) {
        float dx = deltaX(other);
        float dy = deltaY(other);
        float dz = deltaZ(other);
        return Math.round((float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public Heading headingTo(Position other) {
        double dx = deltaX(other);
        double dy = deltaY(other);
        double distanceXY = Math.hypot(dx, dy);

        double sin = dy / distanceXY;
        double cos = dx / distanceXY;

        return new Heading(Math.atan2(-sin, -cos) * 10430.378350470453 + 32768);
    }

    /**
     * Returns true if the distance between positions is lesser than provided
     */
    public boolean isCloseTo(Position other) {
        return isCloseTo(other, 0);
    }

    /**
     * Returns true if the distance between positions is lesser than provided
     */
    public boolean isCloseTo(Position other, int distance) {
        if (other == null) return false;
        return distanceTo(other) <= distance + GEO_CELL_SIZE;
    }

    /**
     * Calculates position between {@code this} and {@code other},
     * which is {@code distanceToTarget} away from {@code other}
     *
     * @return If {@code distanceToTarget} is 0 - copy of {@code other} position
     * If {@code distanceToTarget} is greater than real distance between positions - copy of {@code this} position
     */
    public Position positionBetween(Position other, int distanceToTarget) {
        if (distanceToTarget == 0) {
            return new Position(other.x, other.y, other.z);
        }

        double dx = deltaX(other);
        double dy = deltaY(other);
        double distanceXY = Math.hypot(dx, dy);

        if (distanceXY < distanceToTarget) {
            return new Position(x, y, z);
        }

        double sin = dy / distanceXY;
        double cos = dx / distanceXY;

        int deltaYD = (int) Math.round((distanceXY - distanceToTarget) * sin);
        int deltaXD = (int) Math.round((distanceXY - distanceToTarget) * cos);

        return new Position(x + deltaXD, y + deltaYD, z);
    }

    @NotNull
    @Override
    public String toString() {
        return "Position(x=" + x + ", y=" + y + ", z=" + z + ")";
    }
}
