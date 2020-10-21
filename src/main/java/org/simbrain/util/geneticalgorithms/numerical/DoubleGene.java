package org.simbrain.util.geneticalgorithms.numerical;

import org.simbrain.util.geneticalgorithms.Gene;
import org.simbrain.util.math.SimbrainRandomizer;

/**
 * A double-valued gene.
 */
public class DoubleGene extends Gene<Double> {

    /**
     * Maximum value for gene mutations.
     */
    private double maximum = 10;

    /**
     * Minimum value for gene mutations.
     */
    private double minimum = -10;

    /**
     * Maximum value for gene mutations.
     */
    private double stepSize = .01;

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

        value +=  SimbrainRandomizer.rand.nextDouble(-stepSize, stepSize);

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

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getStepSize() {
        return stepSize;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
