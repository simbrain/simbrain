package org.simbrain.world.odorworld.sensors;

import java.util.List;

public interface Sensor {

    public void update();
    
    public List<Class> getApplicableTypes();
    
}
