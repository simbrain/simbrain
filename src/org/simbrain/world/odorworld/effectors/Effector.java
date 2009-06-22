package org.simbrain.world.odorworld.effectors;

import java.util.List;

public interface Effector {

    public void activate();
    
    public List<Class> getApplicableTypes();
    
}
