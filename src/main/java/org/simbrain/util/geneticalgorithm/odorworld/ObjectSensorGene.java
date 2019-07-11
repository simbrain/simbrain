package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;

public class ObjectSensorGene extends SensorGene {

    private ObjectSensor prototype = new ObjectSensor();

    @Override
    public ObjectSensorGene copy() {
        ObjectSensorGene cpy = new ObjectSensorGene();
        cpy.prototype = this.prototype.copy();
        return cpy;
    }

    @Override
    public ObjectSensor getPrototype() {
        return prototype;
    }

    @Override
    public void mutate() {
        super.mutate();
    }
}
