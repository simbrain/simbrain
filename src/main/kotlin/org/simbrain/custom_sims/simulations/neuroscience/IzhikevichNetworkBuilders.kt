/**
 * The Izhikevich (2003) network of the two Izhikevich demos as free neurons and synapses or as a neuron array and
 * weight matrix. Kept apart from the demos' simulation definitions so the headless benchmark can build the network
 * without loading the simulation machinery, which would otherwise count towards its build time.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichMatrixData
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.IzhikevichScalarData
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.random.Random

private const val EXCITATORY_FRACTION = 0.8

/**
 * Izhikevich (2003) neurons: the first 80% excitatory regular-spiking to chattering cells, the rest inhibitory
 * fast-spiking to low-threshold cells, each with its own random parameter r and a Gaussian noise current.
 */
fun izhikevichNeurons(n: Int, rng: Random): List<Neuron> {
    val numExcitatory = (n * EXCITATORY_FRACTION).toInt()
    return List(n) { i ->
        val r = rng.nextDouble()
        val rule = if (i < numExcitatory) excitatoryIzhikevichRule(r) else inhibitoryIzhikevichRule(r)
        Neuron(rule).apply {
            activation = -65.0
            (dataHolder as IzhikevichScalarData).recovery = rule.b * -65.0
        }
    }
}

/**
 * Random connections between [neurons] with probability [p] and no self-connections. Weights are 0.5 U(0, 1) from
 * excitatory sources and -U(0, 1) from inhibitory ones, times [weightScale], with responses lasting one time step.
 */
fun izhikevichSynapses(neurons: List<Neuron>, p: Double, rng: Random, weightScale: Double = 1.0): List<Synapse> {
    val numExcitatory = (neurons.size * EXCITATORY_FRACTION).toInt()
    val synapses = ArrayList<Synapse>()
    for (src in neurons.indices) {
        for (tgt in neurons.indices) {
            if (src == tgt || rng.nextDouble() >= p) continue
            val w = izhikevichWeight(src < numExcitatory, rng, weightScale)
            synapses += Synapse(neurons[src], neurons[tgt]).apply {
                upperBound = 100.0
                lowerBound = -100.0
                forceSetStrength(w)
                spikeResponder = StepResponder(1)
            }
        }
    }
    return synapses
}

/**
 * The same network as a neuron array with a recurrent weight matrix. An array has one update rule, so every neuron
 * takes the excitatory regular-spiking parameters; the weights keep the 80/20 excitatory/inhibitory split.
 */
fun izhikevichArray(n: Int, p: Double, rng: Random, weightScale: Double = 1.0): Pair<NeuronArray, WeightMatrix> {
    val numExcitatory = (n * EXCITATORY_FRACTION).toInt()
    // r = 0: regular spiking
    val rule = excitatoryIzhikevichRule(0.0)
    val array = NeuronArray(n).apply {
        label = "Izhikevich neurons"
        updateRule = rule
    }
    for (i in 0 until n) {
        array.activations[i, 0] = -65.0
        (array.dataHolder as IzhikevichMatrixData).recoveryMatrix[i, 0] = rule.b * -65.0
    }
    val weights = WeightMatrix(array, array).apply { spikeResponder = StepResponder(1) }
    for (tgt in 0 until n) {
        for (src in 0 until n) {
            weights.weights[tgt, src] = if (src == tgt || rng.nextDouble() >= p) {
                0.0
            } else {
                izhikevichWeight(src < numExcitatory, rng, weightScale)
            }
        }
    }
    return array to weights
}

/**
 * An excitatory cell: regular spiking at [r] = 0, chattering at [r] = 1, with a Gaussian noise current of standard
 * deviation 5.
 */
private fun excitatoryIzhikevichRule(r: Double) = izhikevichRule(5.0).apply {
    a = 0.02; b = 0.2; c = -65.0 + 15 * r * r; d = 8.0 - 6 * r * r
}

/**
 * An inhibitory cell: fast spiking at [r] = 0, low-threshold spiking at [r] = 1, with a Gaussian noise current of
 * standard deviation 2.
 */
private fun inhibitoryIzhikevichRule(r: Double) = izhikevichRule(2.0).apply {
    a = 0.02 + 0.08 * r; b = 0.25 - 0.05 * r; c = -65.0; d = 2.0
}

private fun izhikevichRule(noiseDeviation: Double) = IzhikevichRule().apply {
    backgroundCurrent = 0.0
    threshold = 30.0
    addNoise = true
    noiseGenerator = NormalDistribution(0.0, noiseDeviation)
}

/** 0.5 U(0, 1) from an excitatory source and -U(0, 1) from an inhibitory one, times [weightScale]. */
private fun izhikevichWeight(excitatory: Boolean, rng: Random, weightScale: Double) =
    weightScale * if (excitatory) 0.5 * rng.nextDouble() else -rng.nextDouble()
