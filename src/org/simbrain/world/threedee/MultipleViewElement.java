package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.intersection.BoundingCollisionResults;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

public abstract class MultipleViewElement<T extends Spatial> implements Element {

    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();
    private Node parent;
    
    protected Node getParent() {
        return parent;
    }
    
    public BoundingCollisionResults getCollisions() {
        BoundingCollisionResults data = new BoundingCollisionResults();
        
        for (T spatial : spatials.values()) {
            spatial.findCollisions(parent, data);
            break;
        }
        
        return data;
    }
    
    public void init(Renderer renderer, Node parent) {
        this.parent = parent;
        T spatial = create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        initSpatial(renderer, spatial);
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
