package org.simbrain.world.threedee;

import com.jme.renderer.Camera;

/**
 * Interface of an element that can be the vantage point of a view.
 * 
 * @author Matt Watson
 */
public interface Viewable {
    /**
     * Updates the view.
     */
    void updateView();

    /**
     * Renders the view.
     * 
     * @param camera the camera
     */
    void render(Camera camera);
}
