package org.simbrain.world.threedee.gui;

import org.simbrain.world.threedee.Moveable;

import com.jme.math.Vector3f;

/**
 * A view that can be moved around freely in three dimensions.
 * 
 * @author Matt Watson
 */
public class FreeBirdView extends Moveable {
    /** The current direction of the view. */
    private Vector3f direction;

    /** The current location of the view. */
    private Vector3f location;

    /**
     * Returns the current direction.
     */
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    /**
     * Returns the current location.
     */
    @Override
    protected Vector3f getLocation() {
        return location;
    }

    /**
     * Initializes the view with the given direction and location.
     */
    @Override
    public void init(final Vector3f direction, final Vector3f location) {
        this.direction = direction;
        this.location = location.add(0, 25f, 0);
    }

    /**
     * Sets the current location.
     */
    @Override
    protected void updateDirection(final Vector3f direction) {
        this.direction = direction;
    }

    /**
     * Sets the current direction.
     */
    @Override
    protected void updateLocation(final Vector3f location) {
        this.location = location;
    }
}
