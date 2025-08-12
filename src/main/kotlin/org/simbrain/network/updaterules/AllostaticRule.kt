package org.simbrain.network.updaterules

import org.simbrain.custom_sims.simulations.AllostaticDataHolder
import org.simbrain.custom_sims.simulations.getAllostaticInput
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter
import java.lang.Double.max

/**
 * From Falandays' et al. 2021. Add Homeostasis with adjustible set point
 *
 * Each node is characterized by 4 variables:
 * (1) a current activation level xn, initialized at 0;
 * (2) a fixed leak rate lr of 0.75 (e.g. if the activation level of a node is 1 at time t, the activation level will
 * be 0.75 at time t + 1, + in the absence of further input);
 * (3) a variable target activation level, initialized at Tn = 1;
 * (4) and a variable spiking threshold T’n, which was = always equal to 2Tn
 */
class AllostaticUpdateRule : SpikingNeuronUpdateRule<AllostaticDataHolder, SpikingMatrixData>() {

    @UserParameter(label = "leakRate")
    var leakRate = .75

    @UserParameter(label = "learning rate")
    var learningRate = .01

    override fun createScalarData(): AllostaticDataHolder = AllostaticDataHolder()

    context(Network)
    override fun apply(neuron: Neuron, data: AllostaticDataHolder) {

        // Equation 1
        val newActivation = neuron.activation * leakRate + neuron.getAllostaticInput()
        neuron.activation = max(0.0, newActivation) // Prevent from going below 0

        // Only apply learning if neuron has just spiked
        neuron.isSpike = false

        // Equation 2
        if (neuron.activation > data.threshold) {
            neuron.isSpike = true
            // println("Spike!")
            // Equation 3
            neuron.activation -= data.threshold
        }

        val error = neuron.activation - data.target

        // Weights
        val toTrain = neuron.fanIn
            .filter { it.source.updateRule is SpikingNeuronUpdateRule<*, *> }
            .filter { it.source.isSpike }

        toTrain.forEach { s ->
            if (toTrain.isNotEmpty()) {
                s.strength -= error / toTrain.size
            }
        }

        data.target += error * learningRate
        // Minimum target is 1
        data.target = max(data.target, 1.0)
        data.threshold = 2 * data.target

        // println("target = ${n.target}, threshold = ${n.threshold}, activation = ${n.activation}")
    }

    override fun copy(): AllostaticUpdateRule {
        val copy = AllostaticUpdateRule()
        copy.leakRate = leakRate
        copy.learningRate = learningRate
        return copy
    }

    override val name = "Allostatic Update Rule"

    // Test getSpikingInput
    fun main() {
        with(Network()) {
            val n1 = Neuron()
            val n2 = Neuron()
            addNetworkModelsAsync(n1, n2)
            n1.clamped = true
            n2.clamped = true
            val n3 = Neuron()
            addNetworkModelAsync(n3)
            val s1 = Synapse(n1, n3)
            s1.strength = 1.0
            val s2 = Synapse(n2, n3)
            s2.strength = .5
            addNetworkModelsAsync(s1, s2)
            n1.isSpike = true
            n2.isSpike = true
            println(n3.getAllostaticInput())
        }
    }

}