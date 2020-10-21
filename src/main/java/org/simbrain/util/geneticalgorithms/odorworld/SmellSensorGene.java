package org.simbrain.util.geneticalgorithms.odorworld;

import org.simbrain.world.odorworld.sensors.SmellSensor;

public class SmellSensorGene extends SensorGene {

    private SmellSensor prototype;

    public SmellSensorGene() {
        prototype = new SmellSensor();
    }

    @Override
    public SmellSensorGene copy() {
        SmellSensorGene cpy = new SmellSensorGene();
        cpy.prototype = this.prototype;
        return cpy;
    }

    @Override
    public SmellSensor getPrototype() {
        return prototype;
    }
}
