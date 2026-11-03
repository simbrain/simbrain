package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter
import kotlin.math.exp

/**
 * Spike pairs further apart than this many time constants are ignored, as in most STDP implementations. The
 * exponential has fallen below 1% by then, so this only removes tiny changes from spikes long past.
 */
private const val WINDOW_IN_TAUS = 5.0

/**
 * Models spike time dependent plasticity STDP.
 *
 * Assumes source and target neurons are spiking neurons.
 *
 * See [StdpSim.kt] for more information about the rule.
 *
 * Sources: Jean-Philippe Thivierge and Paul Cisek (2008), Nonperiodic Synchronization in Heterogeneous Networks of  Spiking Neurons
 * and the Scholarpedia article on STDP.
 *
 * Also on anti-hebbian stdp: https://journals.physiology.org/doi/pdf/10.1152/jn.00551.2006
 */
open class STDPRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData> {

    @UserParameter(
        label = "Tau plus",
        description = "Time constant for LTP (weight strengthening when pre fires before post. Smaller values narrow the window within which LTP is applied.",
        increment = .1,
        order = 10)
    var tauPlus: Double = 30.0

    // Often wider than tau plus in the literature
    @UserParameter(
        label = "Tau minus",
        description = "Time constant for LTD (weight decay when post fires before pre). Smaller values narrow the window within which LTD is applied.",
        increment = .1,
        minimumValue = 0.0,
        order = 20)
    var tauMinus: Double = 60.0

    @UserParameter(
        label = "W+",
        description = "Learning rate for LTP case. Controls the magnitude of LTP changes.",
        increment = .1,
        minimumValue = 0.0,
        order = 30
    )
    open var wPlus: Double = 10.0

    @UserParameter(
        label = "W-",
        description = "Learning rate for LTD. Controls magnitude of LTD changes",
        increment = .1,
        minimumValue = 0.0,
        order = 40
    )
    open var wMinus: Double = 10.0

    @UserParameter(
        label = "Learning rate",
        description = "Global learning rate",
        increment = .1,
        minimumValue = 0.0,
        order = 50)
    var learningRate: Double = 0.01

    @UserParameter(
        label = "Hebbian",
        description = "If true, use hebbian learning, else anti-hebbian",
        order = 60
    )
    var isHebbian: Boolean = true

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "STDP"

    constructor()

    constructor(
        w_plus: Double,
        w_minus: Double,
        tau_plus: Double,
        tau_minus: Double,
        learningRate: Double,
        continuous: Boolean
    ) {
        this.wPlus = w_plus
        this.wMinus = w_minus
        this.tauPlus = tau_plus
        this.tauMinus = tau_minus
        this.learningRate = learningRate
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        val duplicateSynapse = STDPRule()
        duplicateSynapse.tauMinus = tauMinus
        duplicateSynapse.tauPlus = tauPlus
        duplicateSynapse.wMinus = wMinus
        duplicateSynapse.wPlus = wPlus
        duplicateSynapse.learningRate = learningRate
        duplicateSynapse.isHebbian = isHebbian
        return duplicateSynapse
    }

    open var deltaW: Double = 0.0

    /**
     * Weight change for a pair of spikes [deltaT] apart (source minus target spike time, sign flipped when
     * anti-Hebbian). Pairs further apart than [WINDOW_IN_TAUS] time constants, simultaneous spikes, and pairs where a
     * neuron has not spiked yet change nothing.
     */
    private fun weightChange(deltaT: Double) = when {
        deltaT < 0 && -deltaT <= WINDOW_IN_TAUS * tauPlus -> wPlus * exp(deltaT / tauPlus) * learningRate
        deltaT > 0 && deltaT <= WINDOW_IN_TAUS * tauMinus -> -wMinus * exp(-deltaT / tauMinus) * learningRate
        else -> 0.0
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        if (synapse.source.isSpike || synapse.target.isSpike) {
            val deltaT = ((synapse.source.lastSpikeTime
                    - synapse.target.lastSpikeTime)
                    * (if (isHebbian) 1 else -1))
            deltaW = weightChange(deltaT)
            synapse.strength += deltaW * timeStep
        }
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        val weightMatrix = connector as? WeightMatrix ?: return
        val sourceNeuronArray = weightMatrix.source as? NeuronArray ?: return
        val targetNeuronArray = weightMatrix.target as? NeuronArray ?: return
        
        // Ensure both neuron arrays have spiking data
        val sourceSpikingData = sourceNeuronArray.dataHolder as? SpikingMatrixData ?: return
        val targetSpikingData = targetNeuronArray.dataHolder as? SpikingMatrixData ?: return
        
        // Only apply if either source or target neurons have spiked
        val hasSourceSpikes = sourceSpikingData.spikes.any { it }
        val hasTargetSpikes = targetSpikingData.spikes.any { it }
        
        if (hasSourceSpikes || hasTargetSpikes) {
            // Get spike times
            val sourceSpikeTimes = sourceSpikingData.lastSpikeTimes
            val targetSpikeTimes = targetSpikingData.lastSpikeTimes
            
            // Apply STDP rule to each connection
            for (i in 0 until weightMatrix.weights.nrow()) { // target neurons
                for (j in 0 until weightMatrix.weights.ncol()) { // source neurons
                    // Only update if at least one neuron has spiked
                    if (sourceSpikingData.spikes[j] || targetSpikingData.spikes[i]) {
                        val deltaT = ((sourceSpikeTimes[j] - targetSpikeTimes[i]) 
                                * (if (isHebbian) 1 else -1))
                        
                        weightMatrix.weights[i, j] += weightChange(deltaT) * timeStep
                    }
                }
            }
        }
    }
}
