package org.simbrain.world.threedee;

import java.util.HashMap;
import java.util.Map;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

public class MultipleViewHandler<T extends Spatial> {//implements Element {
    private final MultipleViewElement<T> multiElement;
    
    public MultipleViewHandler(MultipleViewElement<T> multiElement) {
        this.multiElement = multiElement;
    }
     
    private Map<Renderer, T> spatials = new HashMap<Renderer, T>();
    private Node parent;
    
    public Node getParent() {
        return parent;
    }
        
    public void init(Renderer renderer, Node parent) {    
        this.parent = parent;
        T spatial = multiElement.create();
        spatials.put(renderer, spatial);
        parent.attachChild(spatial);
        multiElement.initSpatial(renderer, spatial);
    }
    
    boolean modelInitialized = false;
    
//    public void init(Vector3f direction, Vector3f location) {
//        if (!modelInitialized) multiElement.initModel(direction, location);
//        
//        for (T spatial : spatials.values()) {
//            spatial.setLocalTranslation(location);
//            spatial.updateWorldBound();
//        }
//    }
    
//    public void update() {
//        multiElement.updateModel();
//        
//        for (T spatial : spatials.values()) {
//            multiElement.updateSpatial(spatial);
//        }
//    }

//    public void commit() {
//        multiElement.commitModel();
//    }

    public SpatialData getTenative() {
        return multiElement.getTenative();
    }

    public void collision(Collision collision) {
        multiElement.collision(collision);
    }
}
