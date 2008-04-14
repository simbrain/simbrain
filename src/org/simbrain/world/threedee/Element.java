package org.simbrain.world.threedee;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;

/**
 * An interface for visible element that can be added to an environment.
 * 
 * @author Matt Watson
 */
public interface Element {
    /**
     * Initializes the element with the given renderer and parent node.
     * 
     * @param renderer a renderer that will be rendering this node
     * @param parent the parent node for this element
     */
    void init(Renderer renderer, Node parent);

    /**
     * Signals that the element should update it's viewable state tentatively.
     */
    void update();

    /**
     * Fired when a collision event occurs.
     * 
     * @param collision the collision data
     */
    void collision(Collision collision);

    /**
     * Signals that the changes in the last updated should be finalized.
     */
    void commit();
    
    /**
     * Gets the tentative data for the element after a collision.
     * 
     * @return the tentative spatial data
     */
    SpatialData getTentative();
}
