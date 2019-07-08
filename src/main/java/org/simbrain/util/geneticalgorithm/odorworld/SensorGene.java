package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NeatUtils;
import org.simbrain.world.odorworld.sensors.Sensor;

public abstract class SensorGene extends Gene<Sensor> {

    @Override
    public abstract Sensor getPrototype();

    @Override
    public void mutate() {
        double newRadius = SimbrainRandomizer.rand.mutateNumberWithProbability(
                getPrototype().getRadius(), config().radiusMaxMutation, config().radiusMutationProbability
        );
        getPrototype().setRadius(NeatUtils.clipping(newRadius, config().radiusMin, config().radiusMax));

        double newTheta = SimbrainRandomizer.rand.mutateNumberWithProbability(
                getPrototype().getTheta(), config().thetaMaxMutation, config().thetaMutationProbability
        );
        getPrototype().setTheta(newTheta);
    }

    public abstract Config config();

    public class Config<C extends SensorGene.Config> {
        private double thetaMaxMutation = 0.314;

        private double thetaMutationProbability = 0.5;

        private double radiusMaxMutation = 5;

        private double radiusMax = 50;

        private double radiusMin = 0;

        private double radiusMutationProbability = 0.5;

        public C thetaMaxMutation(double thetaMaxMutation) {
            this.thetaMaxMutation = thetaMaxMutation;
            return (C) this;
        }

        public C thetaMutationProbability(double thetaMutationProbability) {
            this.thetaMutationProbability = thetaMutationProbability;
            return (C) this;
        }

        public C radiusMaxMutation(double radiusMaxMutation) {
            this.radiusMaxMutation = radiusMaxMutation;
            return (C) this;
        }

        public C radiusMax(double radiusMax) {
            this.radiusMax = radiusMax;
            return (C) this;
        }

        public C radiusMin(double radiusMin) {
            this.radiusMin = radiusMin;
            return (C) this;
        }

        public C radiusMutationProbability(double radiusMutationProbability) {
            this.radiusMutationProbability = radiusMutationProbability;
            return (C) this;
        }

        public SensorGene done() {
            return SensorGene.this;
        }
    }
}
