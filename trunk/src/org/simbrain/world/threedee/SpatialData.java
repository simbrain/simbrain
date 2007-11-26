package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

/**
 * A simple abstraction for holding the temporary spatial data used after
 * updating an element. This can be expanded upon as needed.
 * 
 * @author Matt Watson
 */
public class SpatialData {
    private final Vector3f center;

    private final float radius;

    public SpatialData(final Vector3f center, final float radius) {
        this.center = center;
        this.radius = radius;
    }

    /**
     * Gets the center point of the element.
     * 
     * @return the center point
     */
    public Vector3f centerPoint() {
        return center;
    }

    /**
     * Gets the radius from the center point.
     * 
     * @return the radius
     */
    public float radius() {
        return radius;
    }
}
