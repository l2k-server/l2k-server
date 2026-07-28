package org.l2kserver.game.model.actor;

public record CollisionBox(double radius, double height) {

    public CollisionBox() {
        this(0, 0);
    }

}
