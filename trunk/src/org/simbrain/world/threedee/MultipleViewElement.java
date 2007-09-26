package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

public abstract class MultipleViewElement<T extends Spatial> implements Element {

    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();
    protected Node parent;
        
    public void init(Renderer renderer, Node parent) {
        this.parent = parent;
        T spatial = create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        initSpatial(renderer, spatial);
    }
        
    abstract void initSpatial(Renderer renderer, T spatial);
        
    public abstract T create();

    public void update() {
        for (T spatial : spatials.values()) {
            updateSpatial(spatial);
        }
    }
    
    public abstract void updateSpatial(T spatial);
}
