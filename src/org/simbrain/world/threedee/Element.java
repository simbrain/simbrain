package org.simbrain.world.threedee;

import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;

public interface Element {
    void init(Renderer renderer, Node parent);
    
    void update();
    
    void collision(Collision collision);
    
    float getSpeed();
    
    Vector3f getDirection();
    
    SpatialData getSpatialData();
}
