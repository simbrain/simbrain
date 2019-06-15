package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.geneticalgorithm.testsims.NumberMatching;

public class DoubleGene extends Gene<Double> {

    /**
     * Minimum value for gene mutations.
     */
    private double minimum = Double.MIN_VALUE;

    /**
     * Maximum value for gene mutations.
     */
    private double maximum = Double.MAX_VALUE;

    /**
     * Maximum value for gene mutations.
     */
    private double stepSize = .1;

    /**
     * Current value of the gene
     */
    private Double value;

    /**
     * Create a new double gene initialized to some value.
     *
     * @param value initial value
     */
    public DoubleGene(Double value) {
        this.value = value;
    }

    /**
     * Create a new double gene initialized to 0.
     */
    public DoubleGene() {
        this(0.0);
    }

    @Override
    public void mutate() {

        value += getRandomizer().nextDouble(-stepSize, stepSize);

        // Clip max and min
        if (value < minimum) {
            value = minimum;
        }
        if (value > maximum ) {
            value = maximum;
        }
    }

    @Override
    public DoubleGene copy() {
        DoubleGene newGene = new DoubleGene(value);
        newGene.maximum = maximum;
        newGene.minimum = minimum;
        newGene.stepSize = stepSize;
        return newGene;
    }

    @Override
    public Double getPrototype() {
        return value;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }
}
