package org.simbrain.network.util;

import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Producible;

/**
 * Manages subsampling / downsampling activations of an {@link AbstractNeuronCollection}.
 */
public class SubsamplingManager {

    // TODO: Provide more options for subsampling
    // TODO: Add a function that returns a list of references to subsampled neurons
    /**
     * Number of subsamples to take. This value is also implicitly a threshold.
     * If a neuron group has more than this many neurons, and subsampling is
     * turned on, a vector with this many components is returned by (
     * {@link #getActivations()}
     */
    @UserParameter(label = "Number of subsamples", useSetter = true)
    private int numSubSamples = 100;

    /**
     * Indices used with subsampling.
     */
    private int[] subsamplingIndices = {};

    /**
     * Array to hold subsamples to be used when, for example, plotting the
     * state of large network.
     */
    private double[] subSampledValues = {};

    /**
     * The collection to be subsampled from.
     */
    private final AbstractNeuronCollection nc;

    /**
     * Construct a subsampling manager.
     */
    public SubsamplingManager(AbstractNeuronCollection parentCollection) {
        this.nc = parentCollection;
        setNumSubSamples(100);
    }

    /**
     * Returns a vector of subsampled activations to be used by some object external to the
     * neuron group. If plotting activations of a thousand
     * node network, a sample of 100 activations might be returned.
     *
     * @return the vector of external activations.
     */
    @Producible()
    public double[] getActivations() {
        if (numSubSamples >= nc.getNeuronList().size()) {
            return nc.getActivations();
        }

        if (subSampledValues == null || subSampledValues.length != numSubSamples) {
            subSampledValues = new double[numSubSamples];
        }
        for (int ii = 0; ii < numSubSamples; ii++) {
            subSampledValues[ii] = nc.getNeuron(subsamplingIndices[ii]).getActivation();
        }
        return subSampledValues;
    }

    public int getNumSubSamples() {
        return numSubSamples;
    }

    public void setNumSubSamples(int n) {
        double [] newSubSamples = new double[n];
        int len = n > subsamplingIndices.length ? subsamplingIndices.length : n;
        System.arraycopy(subSampledValues, 0, newSubSamples, 0, len);
        subSampledValues = newSubSamples;
        this.numSubSamples = n;
    }

    /**
     * Reset the indices used for subsampling.
     */
    public void resetIndices() {
        if (nc.getNeuronList() != null) {
            subsamplingIndices = SimbrainMath.randPermute(0, nc.getNeuronList().size());
        }
    }
}
