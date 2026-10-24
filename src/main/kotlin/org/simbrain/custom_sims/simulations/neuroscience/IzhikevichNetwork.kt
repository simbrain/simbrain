/**
 * The 1000-neuron Izhikevich (2003) network as two demos: free neurons in a neuron collection with a recurrent synapse
 * group, and a neuron array with a recurrent weight matrix. Both drive a spike raster. The builders below are also used
 * by the performance benchmarks, so the demo is the same network that is timed; the benchmarks also build it at other
 * sizes through [izhikevichNetworkSim] and [izhikevichNetworkArraySim].
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.updaterules.IzhikevichMatrixData
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.network.updaterules.IzhikevichScalarData
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.NormalDistribution
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

val izhikevichNetwork = izhikevichNetworkSim()

val izhikevichNetworkArray = izhikevichNetworkArraySim()

/**
 * The free-neuron demo with [n] neurons connected with probability [p], every weight multiplied by [weightScale].
 * A grid of about square shape keeps the network window filled at any size.
 */
fun izhikevichNetworkSim(
    n: Int = IZHIKEVICH_NEURONS,
    p: Double = IZHIKEVICH_CONNECTION_PROBABILITY,
    weightScale: Double = 1.0
) = newSim {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Izhikevich Network")
    val network = networkComponent.network
    network.timeStep = IZHIKEVICH_TIME_STEP

    val neurons = izhikevichNeurons(n, Random.Default)
    GridLayout(IZHIKEVICH_GRID_SPACING, IZHIKEVICH_GRID_SPACING, gridColumns(n)).layoutNeurons(neurons)
    network.addNetworkModels(neurons, usePlacementManager = false)
    val collection = NeuronCollection(neurons).apply { label = "Izhikevich neurons" }
    network.addNetworkModel(collection)
    val synapseGroup = SynapseGroup(
        collection,
        collection,
        Sparse(p),
        izhikevichSynapses(neurons, p, Random.Default, weightScale).toMutableList()
    )
    network.addNetworkModel(synapseGroup)

    val raster = addRasterPlot("Spikes")
    with(couplingManager) {
        collection.getProducer(collection::spikes) couple raster.model.rasterConsumerList[0].getConsumer("setValues")
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 800)
        place(raster, SIM_WINDOW_GAP + 800 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 700, 500)
    }

    addSidebarInfo(izhikevichSidebar(arrays = false, n, p))
}

/**
 * The neuron-array demo with [n] neurons connected with probability [p], every weight multiplied by [weightScale].
 */
fun izhikevichNetworkArraySim(
    n: Int = IZHIKEVICH_NEURONS,
    p: Double = IZHIKEVICH_CONNECTION_PROBABILITY,
    weightScale: Double = 1.0
) = newSim {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Izhikevich Network (Array)")
    val network = networkComponent.network
    network.timeStep = IZHIKEVICH_TIME_STEP

    val (array, weights) = izhikevichArray(n, p, Random.Default, weightScale)
    array.location = point(0, 0)
    network.addNetworkModels(array, weights, usePlacementManager = false)

    val raster = addRasterPlot("Spikes")
    with(couplingManager) {
        array.getProducer(array::spikes) couple raster.model.rasterConsumerList[0].getConsumer("setValues")
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 800)
        place(raster, SIM_WINDOW_GAP + 800 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 700, 500)
    }

    addSidebarInfo(izhikevichSidebar(arrays = true, n, p))
}

const val IZHIKEVICH_NEURONS = 1000
const val IZHIKEVICH_CONNECTION_PROBABILITY = 0.1
const val IZHIKEVICH_TIME_STEP = 0.5
private const val IZHIKEVICH_GRID_SPACING = 25.0
private const val EXCITATORY_FRACTION = 0.8

// 32 columns for the 1000-neuron demo
private fun gridColumns(n: Int) = ceil(sqrt(n.toDouble())).toInt()

/**
 * Izhikevich (2003) neurons: the first 80% excitatory regular-spiking to chattering cells, the rest inhibitory
 * fast-spiking to low-threshold cells, each with its own random parameter r and a Gaussian noise current.
 */
fun izhikevichNeurons(n: Int, rng: Random): List<Neuron> {
    val numExcitatory = (n * EXCITATORY_FRACTION).toInt()
    return List(n) { i ->
        val r = rng.nextDouble()
        val rule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
            threshold = 30.0
            addNoise = true
            if (i < numExcitatory) {
                a = 0.02; b = 0.2; c = -65.0 + 15 * r * r; d = 8.0 - 6 * r * r
                noiseGenerator = NormalDistribution(0.0, 5.0)
            } else {
                a = 0.02 + 0.08 * r; b = 0.25 - 0.05 * r; c = -65.0; d = 2.0
                noiseGenerator = NormalDistribution(0.0, 2.0)
            }
        }
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
            val w = weightScale * if (src < numExcitatory) 0.5 * rng.nextDouble() else -rng.nextDouble()
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
    val array = NeuronArray(n).apply {
        label = "Izhikevich neurons"
        updateRule = IzhikevichRule().apply {
            backgroundCurrent = 0.0
            threshold = 30.0
            addNoise = true
            a = 0.02; b = 0.2; c = -65.0; d = 8.0
            noiseGenerator = NormalDistribution(0.0, 5.0)
        }
    }
    for (i in 0 until n) {
        array.activations[i, 0] = -65.0
        (array.dataHolder as IzhikevichMatrixData).recoveryMatrix[i, 0] = 0.2 * -65.0
    }
    val weights = WeightMatrix(array, array).apply { spikeResponder = StepResponder(1) }
    for (tgt in 0 until n) {
        for (src in 0 until n) {
            weights.weights[tgt, src] = if (src == tgt || rng.nextDouble() >= p) {
                0.0
            } else {
                weightScale * if (src < numExcitatory) 0.5 * rng.nextDouble() else -rng.nextDouble()
            }
        }
    }
    return array to weights
}

private fun izhikevichSidebar(arrays: Boolean, n: Int, p: Double) = """
    # Izhikevich Network

    A recurrent network of $n spiking neurons using the simple model of Izhikevich (2003): 80%
    excitatory and 20% inhibitory, each neuron driven by its own Gaussian noise current. Each pair of neurons is
    connected with probability $p, giving about ${"%,d".format(roundToTwoDigits(n * (n - 1) * p))} synapses; excitatory
    weights are positive and inhibitory weights negative, and each spike delivers a one-step current pulse.

    The raster plot shows one row per neuron and a dot for each spike. Activity is low (about 1.4 spikes per second
    per neuron) and irregular, driven by the noise and shaped by the recurrent connections.

    ${if (arrays) {
        "This version stores the neurons in a single neuron array and the synapses in a weight matrix, which is much " +
            "faster to simulate. A neuron array has one update rule, so every neuron here uses the excitatory " +
            "parameters. See the companion simulation for the heterogeneous network as individual neurons."
    } else {
        "This version uses individual neurons, collected in a neuron collection, and individual synapses, collected in " +
            "a synapse group (double-click its arrow to edit the synapses). Excitatory neurons vary from regular " +
            "spiking to chattering and inhibitory neurons from fast spiking to low-threshold spiking. See the " +
            "companion simulation for the same network as a neuron array."
    }}

    The time step is ${IZHIKEVICH_TIME_STEP} ms. This network is also used to benchmark Simbrain's performance.

    Reference: Izhikevich, E. M. (2003). Simple model of spiking neurons. IEEE Transactions on Neural Networks,
    14(6), 1569-1572.
""".trimIndent()

private fun roundToTwoDigits(x: Double): Long {
    if (x < 1) return 0
    val unit = 10.0.pow(floor(log10(x)) - 1)
    return ((x / unit).roundToLong() * unit).roundToLong()
}
