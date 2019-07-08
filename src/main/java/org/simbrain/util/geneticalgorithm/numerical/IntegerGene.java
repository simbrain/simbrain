package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.math.SimbrainRandomizer;

/**
 * An integer-valued gene.  Can be used to
 * represent bit-genes as well by setting min to 0 and max to 1.
 */
public class IntegerGene extends Gene<Integer> {

    /**
     * Maximum value for gene mutations.
     */
    private int maximum = 10;

    /**
     * Minimum value for gene mutations.
     */
    private int minimum = -10;

    /**
     * Maximum value for gene mutations.
     */
    private Integer stepSize = 1;

    /**
     * Current value of the gene
     */
    private Integer value;

    /**
     * Create a new double gene initialized to some value.
     *
     * @param value initial value
     */
    public IntegerGene(Integer value) {
        this.value = value;
    }

    /**
     * Create a new double gene initialized to 0.
     */
    public IntegerGene() {
        this(0);
    }

    @Override
    public void mutate() {

        value +=  SimbrainRandomizer.rand.nextInteger(-stepSize, stepSize);

        // Clip max and min
        if (value < minimum) {
            value = minimum;
        }
        if (value > maximum ) {
            value = maximum;
        }
    }

    @Override
    public IntegerGene copy() {
        IntegerGene newGene = new IntegerGene(value);
        newGene.maximum = maximum;
        newGene.minimum = minimum;
        newGene.stepSize = stepSize;
        return newGene;
    }

    @Override
    public Integer getPrototype() {
        return value;
    }

    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

    public void setStepSize(int stepSize) {
        this.stepSize = stepSize;
    }

    public int getMaximum() {
        return maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    public Integer getStepSize() {
        return stepSize;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
