package org.simbrain.world.threedee;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;

/**
 * An interface for visible element that can be added to an environment
 * 
 * @author Matt Watson
 */
public interface Element {
    /**
     * initializes the element with the given renderer and parent node
     * 
     * @param renderer a renderer that will be rendering this node.
     * @param parent the parent node for this element
     */
    void init(Renderer renderer, Node parent);
    
    /**
     * signals that the element should update it's viewable state
     * tenatively
     */
    void update();
    
    /**
     * fired when a collision event occurs
     * 
     * @param collision
     */
    void collision(Collision collision);
    
    /**
     * signals that the changes in the last updated should be 
     * finalized
     */
    void commit();
    
    /**
     * gets the tenative data for the element after a collision
     * 
     * @return the tenative spatial data
     */
    SpatialData getTenative();
}
