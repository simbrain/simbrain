package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/**
 * An implementation of Log-STDP as introduced in:
 *
 *
 * Gilson M, Fukai T (2011) Stability versus Neuronal Specialization for STDP:
 * Long-Tail Weight Distributions Solve the Dilemma. PLoS ONE 6(10): e25339
 * doi:10.1371/journal.pone.0025339
 *
 *
 * Log-STDP pushes weight values toward a log normal distribution. This can
 * help with weight divergence, which is common to Add and Mlt STDP. It also
 * allows for specialization of synapses, and fits very well with experimental
 * data concerning synaptic efficacy.
 *
 * @author Zoë Tosi
 */
class LogSTDPRule : STDPRule() {
    /**
     * The value that for a given synapse, if the absolute value of the synapses
     * weight is below this threshold it is governed by a different LTD rule
     * than if it were being subjected to LTD, but had a weight greater than
     * this value. LTD for weights below this value linearly approach 0 as the
     * weight approaches 0. LTD for weights above this value are related
     * logarithmically to the synapse strength.
     *
     *
     * J_0 in the cited paper.
     */
    var smallWtThreshold: Double = 1.0

    /**
     * A constant for LTP. c_+ in the cited paper.
     */
    override var wPlus: Double = 2.0

    /**
     * A constant for LTD. c_- in the cited paper.
     */
    override var wMinus: Double = 1.0

    /**
     * The degree to which the distribution is pushed logarithmically. Has an
     * effect on how strongly LTD pushes larger weights toward the small weight
     * threshold.
     *
     *
     * alpha in the cited paper.
     */
    var logSaturation: Double = 5.0

    /**
     * A moderating constant for LTP, causing LTP to behave noticeably
     * differently for weights greater than ltpMod * smallWtThreshold.
     *
     *
     * beta in the cited paper.
     */
    var ltpMod: Double = 10.0

    /**
     * The variance of the noise applied to weight changes.
     */
    var noiseVar: Double = 0.6

    override var deltaW: Double = 0.0

    private val dist = NormalDistribution(0.0, noiseVar)

    /**
     * Updates the synapse's strength using Log-STDP.
     */
    context(Network)
    override fun apply(s: Synapse, data: EmptyScalarData) {
        val sourceSpiking = s.source.updateRule.isSpikingRule
        val targetSpiking = s.target.updateRule.isSpikingRule
        if (!sourceSpiking || !targetSpiking) {
            return  // STDP is non-sensical if one of the units doesn't spike...
        }
        //        final double delay = synapse.getDelay() * timeStep;
        //        if (synapse.getStrength() >= 0) {
        val delta_t = s.source.lastSpikeTime - (s.target.lastSpikeTime)

        //        } else {
        //        	delta_t = tar.getLastSpikeTime()
        //        			- (src.getLastSpikeTime());
        //        }
        if (s.strength >= 0) {
            val noise = 1 + dist.sampleDouble()
            if (delta_t < 0) {
                calcW_plusTerm(s)
                deltaW = timeStep * learningRate * (wPlus * exp(delta_t / tauPlus)) * (1 + noise)
            } else if (delta_t > 0) {
                calcW_minusTerm(s)
                deltaW = timeStep * learningRate * (-wMinus * exp(-delta_t / tauMinus)) * (1 + noise)
            } else {
                deltaW = 0.0
            }
        } else if (s.strength <= 0) {
            deltaW = if (delta_t > 0) {
                learningRate * 1.5 * exp(-delta_t / tauPlus)
            } else if (delta_t < 0) {
                learningRate * -1 * exp(delta_t / tauMinus)
            } else {
                0.0
            }
        }
        s.strength -= deltaW
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        val weightMatrix = connector as? WeightMatrix ?: return
        val sourceNeuronArray = weightMatrix.source as? NeuronArray ?: return
        val targetNeuronArray = weightMatrix.target as? NeuronArray ?: return
        
        // Ensure both neuron arrays have spiking update rules
        if (!sourceNeuronArray.updateRule.isSpikingRule || !targetNeuronArray.updateRule.isSpikingRule) {
            return // Log-STDP is non-sensical if one of the arrays doesn't spike
        }
        
        // Ensure both neuron arrays have spiking data
        val sourceSpikingData = sourceNeuronArray.dataHolder as? SpikingMatrixData ?: return
        val targetSpikingData = targetNeuronArray.dataHolder as? SpikingMatrixData ?: return
        
        // Get spike times
        val sourceSpikeTimes = sourceSpikingData.lastSpikeTimes
        val targetSpikeTimes = targetSpikingData.lastSpikeTimes
        
        // Apply Log-STDP rule to each connection
        for (i in 0 until weightMatrix.weights.nrow()) { // target neurons
            for (j in 0 until weightMatrix.weights.ncol()) { // source neurons
                val weight = weightMatrix.weights[i, j]
                val deltaT = sourceSpikeTimes[j] - targetSpikeTimes[i]
                
                val deltaW = if (weight >= 0) {
                    val noise = 1 + dist.sampleDouble()
                    when {
                        deltaT < 0 -> {
                            val adjustedWPlus = calcW_plusTerm(weight)
                            timeStep * learningRate * (adjustedWPlus * exp(deltaT / tauPlus)) * (1 + noise)
                        }
                        deltaT > 0 -> {
                            val adjustedWMinus = calcW_minusTerm(weight)
                            timeStep * learningRate * (-adjustedWMinus * exp(-deltaT / tauMinus)) * (1 + noise)
                        }
                        else -> 0.0
                    }
                } else {
                    when {
                        deltaT > 0 -> learningRate * 1.5 * exp(-deltaT / tauPlus)
                        deltaT < 0 -> learningRate * -1 * exp(deltaT / tauMinus)
                        else -> 0.0
                    }
                }
                
                weightMatrix.weights[i, j] = weight - deltaW
            }
        }
    }

    private fun calcW_plusTerm(s: Synapse): Double {
        wPlus = wPlus * exp(-abs(s.strength) / (smallWtThreshold * ltpMod))
        return wPlus
    }

    /**
     * Weight-based version for matrix implementation
     */
    private fun calcW_plusTerm(weight: Double): Double {
        return wPlus * exp(-abs(weight) / (smallWtThreshold * ltpMod))
    }

    private fun calcW_minusTerm(s: Synapse): Double {
        val wt = abs(s.strength)
        if (wt <= smallWtThreshold) {
            wMinus = wMinus * wt / smallWtThreshold
        } else {
            val numerator = ln(1 + (logSaturation * ((wt / smallWtThreshold) - 1)))
            wMinus = wMinus * (1 + (numerator / logSaturation))
        }
        return wMinus
    }

    /**
     * Weight-based version for matrix implementation
     */
    private fun calcW_minusTerm(weight: Double): Double {
        val wt = abs(weight)
        return if (wt <= smallWtThreshold) {
            wMinus * wt / smallWtThreshold
        } else {
            val numerator = ln(1 + (logSaturation * ((wt / smallWtThreshold) - 1)))
            wMinus * (1 + (numerator / logSaturation))
        }
    }
}
