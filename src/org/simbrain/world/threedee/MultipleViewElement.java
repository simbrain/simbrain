package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

/**
 * Implementation of Element that allows multiple views to render the same
 * element properly by providing distinct spatials to each renderer.
 * 
 * @author Matt Watson
 */
public abstract class MultipleViewElement<T extends Spatial> implements Element {
    /** holds the spatials, keyed by renderer */
    private final Map<Renderer, T> spatials = new HashMap<Renderer, T>();

    public void init(final Renderer renderer, final Node parent) {
        final T spatial = create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        initSpatial(renderer, spatial);
        parent.updateRenderState();
    }

    /**
     * Initializes a single spatial element.
     * 
     * @param renderer the renderer that will render the spatial
     * @param spatial the spatial to initialize
     */
    abstract void initSpatial(Renderer renderer, T spatial);

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
