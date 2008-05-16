package org.simbrain.world.threedee.gui;

import org.simbrain.world.threedee.Moveable;
import org.simbrain.world.threedee.Point;
import org.simbrain.world.threedee.Vector;

import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;

/**
 * A view that can be moved around freely in three dimensions.
 * 
 * @author Matt Watson
 */
public class FreeBirdView extends Moveable {
    /** The height at which the free-bird starts. */
    private static final float START_HEIGHT = 25f;
    
    /** The current direction of the view. */
    private Vector direction;

    /** The current location of the view. */
    private Point location;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector getDirection() {
        return direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
//    @Override
    public void init(Renderer renderer, final Camera cam, int width, int height) {//final Vector direction, final Point location) {
//        this.direction = direction;
//        this.location = location.add(new Vector(0, START_HEIGHT, 0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateDirection(final Vector direction) {
        this.direction = direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateLocation(final Point location) {
        this.location = location;
    }
}
