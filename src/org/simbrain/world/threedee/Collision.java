package org.simbrain.world.threedee;

import com.jme.math.Vector3f;

/**
 * Interface for proving information about collision events.
 * 
 * If collisions become more complex, this can be expanded upon.
 * 
 * @author Matt Watson
 */
public interface Collision {
    /**
     * Retrieves the other element involved in this collision.
     * 
     * @return the other element in the collision
     */
    Element other();

    /**
     * Returns a vector from the center point of the element identifying the point of
     * contact.
     * 
     * @return A vector from the center point of the element identifying the point of
     * contact.
     */
    Vector3f point();
}
