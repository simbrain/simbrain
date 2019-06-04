package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.geneticalgorithm.testsims.NumberMatching;

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
