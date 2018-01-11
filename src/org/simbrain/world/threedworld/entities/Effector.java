package org.simbrain.world.threedworld.entities;

public interface Effector {
    Agent getAgent();

    void update(float tpf);
    
    void delete();
    
    EffectorEditor getEditor();
}
