package org.simbrain.network.updaterules

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.util.*

/**
 * A simple spiking neuron that fires when weighted inputs exceed a threshold.
 * When spiking activation is 1, else it is 0.
 * TODO: Has no documentation.
 */
open class SpikingThresholdRule : SpikingNeuronUpdateRule<SpikingScalarData, SpikingMatrixData>(), NoisyUpdateRule {
    /**
     * Threshold.
     */
    @JvmField
    @UserParameter(
        label = "Threshold",
        description = "Input value above which the neuron spikes.",
        increment = .1,
        order = 1
    )
    var threshold: Double = .5

    /**
     * The noise generating randomizer.
     */
    override var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Whether or not to add noise to the inputs .
     */
    override var addNoise: Boolean = false

    override fun deepCopy(): SpikingThresholdRule {
        val neuron = SpikingThresholdRule()
        neuron.threshold = threshold
        return neuron
    }

    context(Network)
    override fun apply(layer: Layer, dataHolder: SpikingMatrixData) {
        for (i in 0 until layer.outputSize()) {
            if (spikingThresholdRule(layer.inputs[i, 0])) {
                dataHolder.setHasSpiked(i, true, time)
                layer.outputs[i, 0] = 1.0
            } else {
                dataHolder.setHasSpiked(i, false, time)
                layer.outputs[i, 0] = 0.0
            }
        }
    }

    context(Network)
    override fun apply(neuron: Neuron, data: SpikingScalarData) {
        if (spikingThresholdRule(neuron.input)) {
            neuron.isSpike = true
            neuron.activation = 1.0
        } else {
            neuron.isSpike = false
            neuron.activation = 0.0 // Make this a separate variable?
        }
    }

    fun spikingThresholdRule(`in`: Double): Boolean {
        val input = `in` + (if (addNoise) noiseGenerator.sampleDouble() else 0.0)
        return if (input >= threshold) {
            true
        } else {
            false
        }
    }

    override val randomValue: Double
        get() {
            val rand = Random()
            return if (rand.nextBoolean()) 1.0 else 0.0
        }

    override val name: String
        get() = "Spiking Threshold"
}