/**
 * The 1000-neuron Izhikevich (2003) network as two demos: free neurons in a neuron collection with a recurrent synapse
 * group, and a neuron array with a recurrent weight matrix. Both drive a spike raster. The networks come from the
 * builders in IzhikevichNetworkBuilders.kt, which the performance benchmarks also use, so the demo is the same network
 * that is timed; the GUI benchmarks also build it at other sizes through [izhikevichNetworkSim] and
 * [izhikevichNetworkArraySim].
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.AttributeContainer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.reflect.KProperty0

val izhikevichNetwork = izhikevichNetworkSim()

val izhikevichNetworkArray = izhikevichNetworkArraySim()

/**
 * The free-neuron demo with [n] neurons connected with probability [p], every weight multiplied by [weightScale].
 * A grid of about square shape keeps the network window filled at any size. A [seed] builds the same network every
 * time, the one the headless benchmark builds from that seed; without one each run builds a new network.
 */
fun izhikevichNetworkSim(
    n: Int = IZHIKEVICH_NEURONS,
    p: Double = IZHIKEVICH_CONNECTION_PROBABILITY,
    weightScale: Double = 1.0,
    seed: Long? = null
) = izhikevichSim("Izhikevich Network", arrays = false, n, p) { network ->
    val rng = seed?.let { Random(it) } ?: Random.Default
    val neurons = izhikevichNeurons(n, rng)
    GridLayout(IZHIKEVICH_GRID_SPACING, IZHIKEVICH_GRID_SPACING, gridColumns(n)).layoutNeurons(neurons)
    network.addNetworkModels(neurons, usePlacementManager = false)
    val collection = NeuronCollection(neurons).apply { label = "Izhikevich neurons" }
    network.addNetworkModel(collection)
    val synapseGroup = SynapseGroup(
        collection,
        collection,
        Sparse(p),
        izhikevichSynapses(neurons, p, rng, weightScale).toMutableList()
    )
    network.addNetworkModel(synapseGroup)
    collection to collection::spikes
}

/**
 * The neuron-array demo with [n] neurons connected with probability [p], every weight multiplied by [weightScale],
 * built from [seed] as in [izhikevichNetworkSim].
 */
fun izhikevichNetworkArraySim(
    n: Int = IZHIKEVICH_NEURONS,
    p: Double = IZHIKEVICH_CONNECTION_PROBABILITY,
    weightScale: Double = 1.0,
    seed: Long? = null
) = izhikevichSim("Izhikevich Network (Array)", arrays = true, n, p) { network ->
    val (array, weights) = izhikevichArray(n, p, seed?.let { Random(it) } ?: Random.Default, weightScale)
    array.location = point(0, 0)
    network.addNetworkModels(array, weights, usePlacementManager = false)
    array to array::spikes
}

/**
 * What both demos share: a network window whose network [addNetwork] builds, returning the model and property whose
 * spikes drive the raster plot beside it.
 */
private fun izhikevichSim(
    componentName: String,
    arrays: Boolean,
    n: Int,
    p: Double,
    addNetwork: suspend (Network) -> Pair<AttributeContainer, KProperty0<*>>
) = newSim {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent(componentName)
    val network = networkComponent.network
    network.timeStep = IZHIKEVICH_TIME_STEP

    val (spikeSource, spikes) = addNetwork(network)

    val raster = addRasterPlot("Spikes")
    with(couplingManager) {
        spikeSource.getProducer(spikes) couple raster.model.rasterConsumerList[0].getConsumer("setValues")
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 800)
        place(raster, SIM_WINDOW_GAP + 800 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 700, 500)
    }

    addSidebarInfo(izhikevichSidebar(arrays, n, p))
}

const val IZHIKEVICH_NEURONS = 1000
const val IZHIKEVICH_CONNECTION_PROBABILITY = 0.1
const val IZHIKEVICH_TIME_STEP = 0.5
private const val IZHIKEVICH_GRID_SPACING = 25.0

// 32 columns for the 1000-neuron demo
private fun gridColumns(n: Int) = ceil(sqrt(n.toDouble())).toInt()

private fun izhikevichSidebar(arrays: Boolean, n: Int, p: Double) = """
    # Izhikevich Network

    A recurrent network of $n spiking neurons using the simple model of Izhikevich (2003): 80%
    excitatory and 20% inhibitory, each neuron driven by its own Gaussian noise current. Each pair of neurons is
    connected with probability $p, giving about ${"%,d".format(roundToTwoDigits(n.toLong() * (n - 1) * p))} synapses; excitatory
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
