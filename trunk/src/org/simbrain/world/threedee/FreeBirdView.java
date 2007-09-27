package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

/**
 * A view that can be moved around freely in three dimensions
 * 
 * @author Matt Watson
 */
public class FreeBirdView extends Moveable {
    /** the current direction of the view */
    private Vector3f direction;
    /** the current location of the view */
    private Vector3f location;
    
    /**
     * returns the current direction
     */
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    /**
     * returns the current location
     */
    @Override
    protected Vector3f getLocation() {
        return location;
    }

    /**
     * initializes the view with the given direction and location
     */
    @Override
    public void init(Vector3f direction, Vector3f location) {
        this.direction = direction;
        this.location = location;
    }

    /**
     * sets the current location
     */
    @Override
    protected void updateDirection(Vector3f direction) {
        this.direction = direction;
    }

    /**
     * sets the current direction
     */
    @Override
    protected void updateLocation(Vector3f location) {
        this.location = location;
    }
}
