package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.exp

/**
 * A stochastic spiking neuron often used in cortical branching simulations. The timed accumulator normalizes its inputs
 * using the softmax function:
 *
 * soft_max(x_i) = kappa * exp(x_i) / sum[exp(X * b)]; where X is a vector comprised of x_1, ... , N, in the case of this rule
 * x's are synaptic weights. b controls the steepness of the softmax, whereas kappa can be thought of as a global
 * gain parameter.
 *
 * This gives each weight a probability value, and at each time-step, for each weight where a pre-synaptic neuron spikes
 * the timed accumulator fires a spike with a probability equal to the softmax'd value of that weight.
 * If a spike occurs, the neuron cannot spike again for some predetermined refractory period. The neuron can also
 * fire spontaneously with some probability.
 */
class TimedAccumulatorRule : SpikingThresholdRule() {
    /**
     * Refractory period
     */
    @UserParameter(
        label = "Ref. Period",
        description = "The amount of time it takes for this neuron to spike again.",
        increment = 1.0,
        order = 3,
        minimumValue = 0.0
    )
    var maxState: Int = DEFAULT_REF

    /**
     * The probability that this neuron will spike spontaneously regardless of input.
     */
    @UserParameter(
        label = "Spike Prob.",
        description = "Probability that this neuron will fire spontaneously regardless of state or input.",
        increment = 1E-5,
        order = 4,
        minimumValue = 0.0,
        maximumValue = 1.0
    )
    var baseProb: Double = DEFAULT_BASE_PROB

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
    var b: Double = DEFAIULT_B

    /**
     * Makes all weights more likely to cause spikes in the post synaptic neuron.
     */
    @UserParameter(
        label = "Gain",
        description = "Raises or lowers the probability of spikes causing more spikes via weights.",
        increment = 0.1,
        minimumValue = 0.0,
        order = 2
    )
    var kappa: Double = defaultKappa

    /**
     * Sum of all weights when exponentiated: exp(weights * b)
     */
    private var expSum = 0.0

    /**
     * Number of incoming connections.
     */
    private val fanInSize = 0

    /**
     * A timing variable to keep track of refractory periods.
     */
    var currentState: Int = 0

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    context(Network)
    override fun apply(neuron: Neuron, data: SpikingScalarData) {
        if (currentState >= 1) {
            currentState++
            if (currentState > maxState) {
                currentState = 0
            }
            neuron.activation = currentState.toDouble()
            neuron.isSpike = false
            return
        }
        if (ThreadLocalRandom.current().nextDouble() < baseProb) {
            currentState++
            neuron.activation = 1.0
            neuron.isSpike = true
            return
        }
        expSum = 0.0
        for (ii in 0 until fanInSize) {
            if (!neuron.fanInUnsafe!![ii].frozen) expSum += exp(b * neuron.fanInUnsafe!![ii].strength)
        }
        for (ii in 0 until fanInSize) {
            // Skip if pre-synaptic node is off...
            if (neuron.fanIn[ii].source.activation == 1.0) {
                // Using the exp weight value stored in the PSR from before
                // divide that by the exp sum to get the softmax value
                // then set this to a 1 state from a 0 with that probability.
                if (ThreadLocalRandom.current().nextDouble() < kappa * neuron.fanInUnsafe!![ii].psr / expSum) {
                    currentState++
                    neuron.activation = 1.0
                    neuron.isSpike = true
                    return
                }
            }
        }
        neuron.activation = 0.0
        neuron.isSpike = false
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
    override fun copy(): TimedAccumulatorRule {
        val tar = TimedAccumulatorRule()
        tar.baseProb = baseProb
        tar.currentState = currentState
        tar.maxState = maxState
        tar.kappa = kappa
        return tar
    }

    override val name: String
        get() = "Timed Accumulator"

    companion object {
        const val defaultKappa: Double = 0.9
        const val DEFAIULT_B: Double = 1.6
        const val DEFAULT_BASE_PROB: Double = 1E-5
        const val DEFAULT_REF: Int = 10
    }
}