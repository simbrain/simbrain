package org.simbrain.world.odorworld;

import java.awt.*;
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
    }


    @Override
    public void updateCollisionBounds(String direction) {
        double dx = 0;
        double dy = 0;
        if (direction.contains("x")) {
            dx = getVelocity().getX();
        }
        if (direction.contains("y")) {
            dy = getVelocity().getY();
        }
        collisionBounds.get("up").setLine(
                shape.getX() + dx,
                shape.getY() + dy,
                shape.getX() + shape.getWidth() + dx,
                shape.getY() + dy
        );
        collisionBounds.get("down").setLine(
                shape.getX() + dx,
                shape.getY() + shape.getHeight() + dy,
                shape.getX() + shape.getWidth() + dx,
                shape.getY() + shape.getHeight() + dy
        );
        collisionBounds.get("left").setLine(
                shape.getX() + dx,
                shape.getY() + dy,
                shape.getX() + dx,
                shape.getY() + shape.getHeight() + dy
        );
        collisionBounds.get("right").setLine(
                shape.getX() + shape.getWidth() + dx,
                shape.getY() + dy,
                shape.getX() + shape.getWidth() + dx,
                shape.getY() + shape.getHeight() + dy
        );
    }



    @Override
    public void updateCollisionRadius() {
        // approximating the radius using manhattan distance of the collision bound
        double x = shape.getWidth() + Math.abs(getVelocity().getX());
        double y = shape.getHeight() + Math.abs(getVelocity().getY());
        collisionRadius = (x + y) / 2;
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
