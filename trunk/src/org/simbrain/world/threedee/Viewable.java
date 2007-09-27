package org.simbrain.world.threedee;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

/**
 * inteface of an element that can be the vantage point
 * of a view
 * 
 * @author Matt Watson
 */
public interface Viewable {
    /**
     * initializes the view
     * 
     * @param direction the direction
     * @param location the location
     */
    void init(Vector3f direction, Vector3f location);
    
    /**
     * updates the view
     */
    void updateView();
    
    /**
     * renders the view
     * 
     * @param camera the camera
     */
    void render(Camera camera);
}
