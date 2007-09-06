package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

public class SpatialData {
    private final Vector3f center;
    private final float radius;
    
    public SpatialData(Vector3f center, float radius) {
        this.center = center;
        this.radius = radius;
    }
    
    /**
     * gets the center point of the element
     * 
     * @return the center point
     */
    Vector3f centerPoint() {
        return center;
    }
    
    /**
     * gets the radius from the center point
     * 
     * @return the radius
     */
    float radius() {
        return radius;
    }
}
