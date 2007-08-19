package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

public abstract class MultipleViewElement<T extends Spatial> implements Element {

    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();
    
    public void init(Renderer renderer, Node parent) {
        T spatial = create();
        spatials.put(renderer, spatial);
        initSpatial(renderer, spatial);
        parent.attachChild(spatial);
    }
    
    protected abstract void initSpatial(Renderer renderer, T spatial);
    
    public void init(Vector3f direction, Vector3f location) {
        for (T spatial : spatials.values()) {
            spatial.setLocalTranslation(location);
            spatial.updateWorldBound();
        }
    }
    
    protected abstract T create();
    
    public void update() {
        for (T spatial : spatials.values()) {
            updateSpatial(spatial);
        }
    }
    
    protected abstract void updateSpatial(T spatial);
}
