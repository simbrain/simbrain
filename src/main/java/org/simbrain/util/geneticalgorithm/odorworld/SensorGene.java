package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NeatUtils;
import org.simbrain.world.odorworld.sensors.Sensor;

public abstract class SensorGene extends Gene<Sensor> {

    private OdorWorldEntityGenome.Config config;

    @Override
    public abstract Sensor getPrototype();

    @Override
    public void mutate() {
        double newRadius = SimbrainRandomizer.rand.mutateNumberWithProbability(
                getPrototype().getRadius(), getConfig().getRadiusMaxMutation(),
                getConfig().getRadiusMutationProbability()
        );
        //getPrototype().setRadius(NeatUtils.clipping(newRadius, getConfig().getRadiusMin(), getConfig().getRadiusMax()));
        getPrototype().setRadius(50);

        double newTheta = SimbrainRandomizer.rand.mutateNumberWithProbability(
                getPrototype().getTheta(), getConfig().getThetaMaxMutation(), getConfig().getThetaMutationProbability()
        );
        getPrototype().setTheta(newTheta);

    }

    public OdorWorldEntityGenome.Config getConfig() {
        return config;
    }

    public void setConfig(OdorWorldEntityGenome.Config config) {
        this.config = config;
    }
}
