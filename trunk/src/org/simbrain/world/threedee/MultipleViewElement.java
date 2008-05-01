package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

/**
 * Implementation of Element that allows multiple views to render the same
 * element properly by providing distinct spatials to each renderer.
 * 
 * @param <T> The spatial type is the root element for this Element.
 * 
 * @author Matt Watson
 */
public abstract class MultipleViewElement<T extends Spatial> implements Element {
    /** Holds the spatials, keyed by renderer. */
    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();

    /**
     * Initializes a renderer and parent as a new view on the Element.
     * 
     * @param renderer The renderer that will render this Element.
     * @param parent The parent Node to which the Element's root node
     *        will be attached.
     */
    public void init(final Renderer renderer, final Node parent) {
        final T spatial = create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        initSpatial(renderer, spatial);
        parent.updateRenderState();
    }

    private Object readResolve() {
        spatials = new HashMap<Renderer, T>();
        
        return this;
    }
    
    /**
     * Initializes a single spatial element.
     * 
     * @param renderer the renderer that will render the spatial
     * @param spatial the spatial to initialize
     */
    public abstract void initSpatial(Renderer renderer, T spatial);

    /**
     * Creates a new spatial.
     * 
     * @return a new spatial
     */
    public abstract T create();

    /**
     * Updates all the spatials.
     */
    public void update() {
        for (final T spatial : spatials.values()) {
            updateSpatial(spatial);
        }
    }

    /**
     * Updates a single spatial.
     * 
     * @param spatial the spatial to update
     */
    public abstract void updateSpatial(T spatial);
}
