package org.simbrain.world.threedee;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

public interface Viewable {
    void init(Vector3f direction, Vector3f location);
    
    void update();
    
    void render(Camera camera);
}
