package org.simbrain.util.neat2;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.neat.ConnectionGene;

import java.awt.geom.Point2D;

public class DoubleGene extends Gene<Double> {

    Double value = 0.0;

    public DoubleGene(Double value) {
        this.value = value;
    }

    @Override
    public void mutate() {

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
