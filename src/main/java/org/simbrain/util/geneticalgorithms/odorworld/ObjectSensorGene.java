package org.simbrain.util.geneticalgorithms.odorworld;

import org.simbrain.world.odorworld.sensors.ObjectSensor;

public class ObjectSensorGene extends SensorGene {

    private ObjectSensor prototype = new ObjectSensor();

    {
        prototype.getDecayFunction().setDispersion(250);
    }

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
