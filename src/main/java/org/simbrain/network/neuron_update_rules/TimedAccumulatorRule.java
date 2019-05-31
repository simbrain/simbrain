package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.UserParameter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A stochastic spiking neuron often used in cortical branching simulations. The timed accumulator normalizes its inputs
 * using the softmax function:
 *
 *      soft_max(x_i) = kappa * exp(x_i) / sum[exp(X * b)]; where X is a vector comprised of x_1, ... , N, in the case of this rule
 *      x's are synaptic weights. b controls the steepness of the softmax, whereas kappa can be thought of as a global
 *      gain parameter.
 *
 * This gives each weight a probability value, and at each time-step, for each weight where a pre-synaptic neuron spikes
 * the timed accumulator fires a spike with a probability equal to the softmax'd value of that weight.
 * If a spike occurs, the neuron cannot spike again for some predetermined refractory period. The neuron can also
 * fire spontaneously with some probability.
 */
public class TimedAccumulatorRule extends SpikingThresholdRule {

    public static final double DEFAULT_KAPPA = 0.9;
    public static final double DEFAIULT_B = 1.6;
    public static final double DEFAULT_BASE_PROB = 1E-5;
    public static final int DEFAULT_REF = 10;

    /**
     * Refractory period
     */
    @UserParameter(
            label = "Ref. Period",
            description = "The amount of time it takes for this neuron to spike again.",
            increment = 1,
            order = 3,
            minimumValue = 0
    )
    private int maxState = DEFAULT_REF;

    /**
     * The probability that this neuron will spike spontaneously regardless of input.
     */
    @UserParameter(
            label = "Spike Prob.",
            description = "Probability that this neuron will fire spontaneously regardless of state or input.",
            increment = 1E-5,
            order = 4,
            minimumValue = 0,
            maximumValue = 1
    )
    private double baseProb = DEFAULT_BASE_PROB;

    /**
     * The shape parameter of the softmax function over the weights. Large positive values with cause extreme disparity
     * between the smallest and larges (or even between the largest and 2nd largest), while as b approaches 0,
     * the probabilities corresponding to each weight converge to the same value. Negative values cause smaller numbers
     * to become larger probabilities and larger numbers to become smaller probabilities.
     */
    @UserParameter(
            label = "Shape Parameter",
            description = "Affects the nonlinearty of the softmax.\nStronger weights will become exponentially more likely to cause spikes.",
            increment = 0.1,
            order = 1
    )
    private double b = DEFAIULT_B;

    /**
     * Makes all weights more likely to cause spikes in the post synaptic neuron.
     */
    @UserParameter(
            label="Gain",
            description="Raises or lowers the probability of spikes causing more spikes via weights.",
            increment = 0.1,
            minimumValue = 0,
            order = 2
    )
    private double kappa = DEFAULT_KAPPA;

    /**
     * Sum of all weights when exponentiated: exp(weights * b)
     */
    private double expSum = 0;

    /**
     * Number of incoming connections.
     */
    private int fanInSize = 0;

    /**
     * A timing variable to keep track of refractory periods.
     */
    private int currentState;

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public void update(Neuron neuron) {
        if (currentState >= 1) {
            currentState++;
            if (currentState > maxState) {
                currentState = 0;
            }
            neuron.setBuffer(currentState);
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < baseProb) {
            currentState++;
            neuron.setBuffer(1);
            neuron.setSpkBuffer(true);
            setHasSpiked(true, neuron);
            return;
        }
        expSum = 0;
        for(int ii=0; ii<fanInSize; ++ii) {
            if(!neuron.getFanInUnsafe().get(ii).isFrozen())
                expSum += Math.exp(b * neuron.getFanInUnsafe().get(ii).getStrength());
        }
        for (int ii = 0; ii < fanInSize; ++ii) {
            // Skip if pre-synaptic node is off...
            if (neuron.getFanIn().get(ii).getSource().getActivation() == 1) {
                // Using the exp weight value stored in the PSR from before
                // divide that by the exp sum to get the softmax value
                // then set this to a 1 state from a 0 with that probability.
                if (ThreadLocalRandom.current().nextDouble() < kappa * neuron.getFanInUnsafe().get(ii).getPsr() / expSum) {
                    currentState++;
                    neuron.setBuffer(1);
                    neuron.setSpkBuffer(true);
                    setHasSpiked(true, neuron);
                    return;
                }
            }
        }
        neuron.setBuffer(0);
        neuron.setSpkBuffer(false);
        setHasSpiked(false, neuron);
    }

//    public void init(Neuron neuron) {
//        fanInSize = neuron.getFanIn().size();
//        // Obtain the exponential sum for the denominator
//        for (Synapse s : neuron.getFanIn()) {
//            double expVal = Math.exp(s.getStrength() * b);
//            expSum += expVal;
//            s.setPsr(expVal); // Store the exp val of the weight for later use
//        }
//    }

    @Override
    public TimedAccumulatorRule deepCopy() {
        TimedAccumulatorRule tar = new TimedAccumulatorRule();
        tar.setBaseProb(baseProb);
        tar.setCurrentState(currentState);
        tar.setMaxState(maxState);
        tar.setKappa(kappa);
        tar.setLastSpikeTime(getLastSpikeTime());
        return tar;
    }

    public int getMaxState() {
        return maxState;
    }

    public void setMaxState(int maxState) {
        this.maxState = maxState;
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
    }

    public double getBaseProb() {
        return baseProb;
    }

    public void setBaseProb(double baseProb) {
        this.baseProb = baseProb;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public static double getDefaultKappa() {
        return DEFAULT_KAPPA;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    @Override
    public String getName() {
        return "Timed Accumulator";
    }

}
