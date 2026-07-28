package org.l2kserver.game.model.utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

public class GeomUtils {

    private static final Random random = new Random();

    private GeomUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static Point2D getRandomPoint(Shape region) {
        Rectangle r = region.getBounds();
        double x, y;

        do {
            x = r.getX() + r.getWidth() * random.nextDouble();
            y = r.getY() + r.getHeight() * random.nextDouble();
        } while (!region.contains(x, y));

        return new Point2D.Double(x, y);
    }
}
