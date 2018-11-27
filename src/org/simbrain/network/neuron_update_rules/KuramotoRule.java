package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.interfaces.*;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * TODO
 * <p>
 * https://en.wikipedia.org/wiki/Kuramoto_model
 * <p>
 * K is weight N = number of fan-in nodes
 * <p>
 * TODO: Contextual increment.  Proper randomize and bounds.
 * Remove un-needed overrides.  Finish GUI.   Include time step in gui.
 */
public class KuramotoRule extends NeuronUpdateRule implements BiasedUpdateRule, DifferentiableUpdateRule, BoundedUpdateRule, ClippableUpdateRule, NoisyUpdateRule {

    /**
     * The Default upper bound.
     */
    private static final double DEFAULT_UPPER_BOUND = 1.0;

    /**
     * The Default lower bound.
     */
    private static final double DEFAULT_LOWER_BOUND = -1.0;

    /**
     * Default clipping setting.
     */
    private static final boolean DEFAULT_CLIPPING = true;

    /**
     * Natural Frequency.
     */
    @UserParameter(
        label = "Natural frequency",
        description = "todo.",
        defaultValue = "0.2", order = 1)
    public double naturalFrequency = 1;

    /**
     * Bias.
     */
    public double bias = 0;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Add noise to the neuron.
     */
    private boolean addNoise = false;

    /**
     * Clipping.
     */
    private boolean clipping = DEFAULT_CLIPPING;

    /**
     * The upper bound of the activity if clipping is used.
     */
    private double upperBound = DEFAULT_UPPER_BOUND;

    /**
     * The lower bound of the activity if clipping is used.
     */
    private double lowerBound = DEFAULT_LOWER_BOUND;

    ///// NEW STUFF ////
    private double timeStep;

    @Override
    public void update(Neuron neuron) {

        timeStep = neuron.getNetwork().getTimeStep();

        double sum = 0;
        for (Synapse s : neuron.getFanIn()) {
            sum += s.getStrength() * Math.sin(s.getSource().getActivation() - neuron.getActivation());
        }
        double N = neuron.getFanIn().size();
        N = (N > 0) ? N : 1;
        double theta_dot = naturalFrequency + sum / N;

        double theta = neuron.getActivation() + (timeStep * theta_dot);
        theta = theta % (2 * Math.PI);

        // if (addNoise) {
        // val += noiseGenerator.getRandom();
        // }
        //
        // if (clipping) {
        // val = clip(val);
        // }

        neuron.setBuffer(theta);
    }

    @Override
    public double clip(double val) {
        if (val > getUpperBound()) {
            return getUpperBound();
        } else if (val < getLowerBound()) {
            return getLowerBound();
        } else {
            return val;
        }
    }

    @Override
    public Network.TimeType getTimeType() {
        return Network.TimeType.DISCRETE;
    }

    @Override
    public KuramotoRule deepCopy() {
        KuramotoRule kr = new KuramotoRule();
        kr.setBias(getBias());
        kr.setSlope(getSlope());
        kr.setClipped(isClipped());
        kr.setAddNoise(getAddNoise());
        kr.setUpperBound(getUpperBound());
        kr.setLowerBound(getLowerBound());
        kr.noiseGenerator = noiseGenerator.deepCopy();
        return kr;
    }

    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act >= getUpperBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act + n.getIncrement());
            } else {
                act = act + n.getIncrement();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    @Override
    public void contextualDecrement(Neuron n) {
        double act = n.getActivation();
        if (act <= getLowerBound() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act - n.getIncrement());
            } else {
                act = act - n.getIncrement();
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    @Override
    public double getDerivative(double val) {
        if (val >= getUpperBound()) {
            return 0;
        } else if (val <= getLowerBound()) {
            return 0;
        } else {
            return naturalFrequency;
        }
    }

    @Override
    public double getBias() {
        return bias;
    }

    @Override
    public void setBias(final double bias) {
        this.bias = bias;
    }

    /**
     * @param slope The slope to set.
     */
    public void setSlope(final double slope) {
        this.naturalFrequency = slope;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    @Override
    public String getName() {
        return "Kuramoto";
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    @Override
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    @Override
    public boolean isClipped() {
        return clipping;
    }

    @Override
    public void setClipped(boolean clipping) {
        this.clipping = clipping;
    }

    /**
     * @return Returns the slope.
     */
    public double getSlope() {
        return naturalFrequency;
    }

    public double getNaturalFrequency() {
        return naturalFrequency;
    }

    public void setNaturalFrequency(double naturalFrequency) {
        this.naturalFrequency = naturalFrequency;
    }

}
