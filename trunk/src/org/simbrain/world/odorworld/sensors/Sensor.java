package org.simbrain.world.odorworld.sensors;

import java.util.List;

public interface Sensor {

    public Double getValue();
    
    public List<Class> getApplicableTypes();
    
}
