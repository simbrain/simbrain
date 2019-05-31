package org.simbrain.world.odorworld;

import org.simbrain.util.Pair;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;

public abstract class CollisionBound {

    /**
     * Approximated collision bound.
     */
    protected double collisionRadius;

    /**
     * Top left location of this collision bound.
     */
    protected Point2D.Double location = new Point2D.Double(0.0, 0.0);

    /**
     * Center location of this collision bound.
     */
    protected Point2D.Double centerLocation = new Point2D.Double(0.0, 0.0);

    /**
     * Actual collision bound.
     */
    protected HashMap<String, Line2D.Double> collisionBounds = new HashMap<>();

    /**
     * Velocity of this collision bound
     */
    private Point2D.Double velocity = new Point2D.Double(0.0, 0.0);


    /**
     * Check if this entity is colliding with other entity in a given direction.
     *
     * @param other     the other entity
     * @return true if collided
     */
    public boolean collide(CollisionBound other) {
        if (isInCollisionRadius(other)) {
            for (Line2D l : collisionBounds.values()) {
                for (Line2D ol : other.collisionBounds.values()) {
                    if (l.intersectsLine(ol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isInCollisionRadius(CollisionBound other) {
        if (other == this) {
            return false;
        }
        double dx = this.centerLocation.getX() - other.centerLocation.getX();
        double dy = this.centerLocation.getY() - other.centerLocation.getY();
        double totalRadius = this.collisionRadius + other.collisionRadius;
        return totalRadius * totalRadius > dx * dx + dy * dy;
    }

    public abstract void updateCollisionBounds(String direction);

    public abstract void updateCollisionRadius();

    public double getCollisionRadius() {
        return collisionRadius;
    }

    public Point2D.Double getVelocity() {
        return velocity;
    }

    public void setVelocity(Point2D.Double velocity) {
        this.velocity = velocity;
    }

    public void setVelocity(double dx, double dy) {
        this.velocity.setLocation(dx, dy);
    }

    public Point2D.Double getLocation() {
        return location;
    }

    public abstract void setLocation(Point2D.Double location);

    public Point2D.Double getCenterLocation() {
        return centerLocation;
    }
}
