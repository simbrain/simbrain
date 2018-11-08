package org.simbrain.world.odorworld;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class RectangleCollisionBound extends CollisionBound {

    private Rectangle2D.Double shape;


    public RectangleCollisionBound(Rectangle2D.Double rect) {
        shape = rect;
        collisionBounds.put("up", new Line2D.Double());
        collisionBounds.put("down", new Line2D.Double());
        collisionBounds.put("left", new Line2D.Double());
        collisionBounds.put("right", new Line2D.Double());
        updateCollisionBounds("xy");
        updateCollisionRadius();
        setLocation(rect.getX(), rect.getY());
    }


    @Override
    public void updateCollisionBounds(String direction) {
        double dxLeft = 0;
        double dxRight = 0;
        double dyUp = 0;
        double dyDown = 0;
        if (direction.contains("x")) {
            if (getVelocity().getX() > 0) {
                dxRight = getVelocity().getX();
            } else {
                dxLeft = getVelocity().getX();
            }
        }
        if (direction.contains("y")) {
            if (getVelocity().getY() > 0) {
                dyDown = getVelocity().getY();
            } else {
                dyUp = getVelocity().getY();
            }
        }
        collisionBounds.get("up").setLine(
                shape.getX() + dxLeft,
                shape.getY() + dyUp,
                shape.getX() + shape.getWidth() + dxRight,
                shape.getY() + dyUp
        );
        collisionBounds.get("down").setLine(
                shape.getX() + dxLeft,
                shape.getY() + shape.getHeight() + dyDown,
                shape.getX() + shape.getWidth() + dxRight,
                shape.getY() + shape.getHeight() + dyDown
        );
        collisionBounds.get("left").setLine(
                shape.getX() + dxLeft,
                shape.getY() + dyUp,
                shape.getX() + dxLeft,
                shape.getY() + shape.getHeight() + dyDown
        );
        collisionBounds.get("right").setLine(
                shape.getX() + shape.getWidth() + dxRight,
                shape.getY() + dyUp,
                shape.getX() + shape.getWidth() + dxRight,
                shape.getY() + shape.getHeight() + dyDown
        );
    }



    @Override
    public void updateCollisionRadius() {
        // the collision radius is approximately the circle enclosing the rectangular bound of this object.
        // 1.5 is about sqrt(2), which is the ratio of length of the diagonal line to the side of a square,
        // and the radius is half of that.
        // the velocity is also taken into account in case of high speed object.
        collisionRadius = Math.max(
                shape.getWidth() + Math.abs(getVelocity().getX()),
                shape.getHeight() + Math.abs(getVelocity().getY())
        ) * 0.75;
    }

    public boolean collide(String direction, CollisionBound other) {
        updateCollisionBounds(direction);
        boolean ret = collide(other);
        updateCollisionBounds("xy");
        return ret;
    }

    @Override
    public void setLocation(Point2D.Double location) {
        setLocation(location.getX(), location.getY());
    }

    public void setLocation(double x, double y) {
        shape.setRect(x, y, shape.getWidth(), shape.getHeight());
        location.setLocation(x, y);
        centerLocation.setLocation(x + shape.getWidth() / 2, y + shape.getHeight() / 2);
        updateCollisionRadius();
        updateCollisionBounds("xy");
    }

    public void setSize(double w, double h) {
        double x = shape.getX();
        double y = shape.getY();
        shape.setRect(x, y, w, h);
        centerLocation.setLocation(x + w / 2, y + h / 2);
        updateCollisionRadius();
        updateCollisionBounds("xy");
    }
}
