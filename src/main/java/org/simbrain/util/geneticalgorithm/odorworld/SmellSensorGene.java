package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.world.odorworld.sensors.SmellSensor;

public class SmellSensorGene extends SensorGene {

    private Config config = new Config();

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

    @Override
    public Config config() {
        return config;
    }

    public class Config extends SensorGene.Config<SmellSensorGene.Config> {
        // no extra parameter here
    }
}
