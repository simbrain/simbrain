package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

/**
 * implementation of Element that allows multiple views to render
 * the same element properly by providing distinct spatials to each 
 * renderer
 * 
 * @author Matt Watson
 *
 * @param <T> the spatial type this object provides
 */
public abstract class MultipleViewElement<T extends Spatial> implements Element {
    /** holds the spatials, keyed by renderer */
    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();
        
    public void init(Renderer renderer, Node parent) {
        T spatial = create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        initSpatial(renderer, spatial);
    }
    
    /**
     * initializes a single spatial element
     * 
     * @param renderer the renderer that will render the spatial
     * @param spatial the spatial to initialize
     */
    abstract void initSpatial(Renderer renderer, T spatial);
    
    /**
     * creates a new spatial
     * 
     * @return a new spatial
     */
    public abstract T create();

    /**
     * updates all the spatials
     */
    public void update() {
        for (T spatial : spatials.values()) {
            updateSpatial(spatial);
        }
    }
    
    /**
     * updates a single spatial
     * 
     * @param spatial the spatial to update
     */
    public abstract void updateSpatial(T spatial);
}
