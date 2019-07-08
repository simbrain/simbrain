package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;

public class ObjectSensorGene extends SensorGene {

    private ObjectSensor prototype = new ObjectSensor();

    private Config config = new Config();

    @Override
    public Config config() {
        return config;
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

    public class Config extends SensorGene.Config<Config> {

        private double baseValueMutationProbability = 0.2;

        private double baseValueMaxMutation = 0.1;

        private double baseValueMin = 0.1;

        private double baseValueMax = 2;

        public Config baseValueMutationProbability(double baseValueMutationProbability) {
            this.baseValueMutationProbability = baseValueMutationProbability;
            return this;
        }

        public Config baseValueMaxMutation(double baseValueMaxMutation) {
            this.baseValueMaxMutation = baseValueMaxMutation;
            return this;
        }

        public Config baseValueMin(double baseValueMin) {
            this.baseValueMin = baseValueMin;
            return this;
        }

        public Config baseValueMax(double baseValueMax) {
            this.baseValueMax = baseValueMax;
            return this;
        }

        @Override
        public ObjectSensorGene done() {
            return ObjectSensorGene.this;
        }
    }
}
