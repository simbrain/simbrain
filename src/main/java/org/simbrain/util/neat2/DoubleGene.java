package org.simbrain.util.neat2;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.ConnectionGene;
import org.simbrain.util.neat2.testsims.NumberMatching;

import java.awt.geom.Point2D;

public class DoubleGene extends Gene<Double> {

    private Double value;

    public DoubleGene(Double value) {
        this.value = value;
    }

    public DoubleGene() {
        this(0.0);
    }

    @Override
    public void mutate() {
        value += getRandomizer().nextDouble(-NumberMatching.MAX_MUTATION, NumberMatching.MAX_MUTATION);
    }

    @Override
    public DoubleGene copy() {
        return new DoubleGene(value);
    }

    @Override
    public Double getPrototype() {
        return value;
    }


}
