package org.simbrain.world.threedee;

import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;

/**
 * Interface of an element that can be the vantage point of a view.
 * 
 * @author Matt Watson
 */
public interface Viewable {
    /**
     * Initializes the view.
     * 
     * @param direction the direction
     * @param location the location
     */
    void init(Renderer renderer, Camera cam, int width, int height);

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
