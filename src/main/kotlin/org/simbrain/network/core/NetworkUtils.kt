package org.simbrain.network.core

import com.thoughtworks.xstream.XStream
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.groups.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * If a subnetwork or synapse group has more than this many synapses, then the initial synapse visibility flag is
 * set false.
 */
@Transient
var synapseVisibilityThreshold = SimbrainPreferences.getInt("networkSynapseVisibilityThreshold")

/**
 * Provides an ordering on [NetworkModels] so that the networks are rebuilt in a proper order, for example with
 * neurons created before synapses (which refer to neurons).
 */
fun reconstructionOrder(obj: NetworkModel): Int = when (obj) {
    is Neuron -> 10
    is NeuronGroup -> 20
    is NeuronCollection -> 30
    is NeuronArray -> 40
    is Connector -> 50
    is SynapseGroup -> 60
    is SynapseGroup2 -> 65
    is Subnetwork -> 70
    is Synapse -> 80
    else -> 55
}

/**
 * Convenience method for asynchronously updating a set of neurons, by calling each neuron's update function (which
 * sets a buffer), and then setting each neuron's activation to the buffer state.
 *
 * @param neuronList the list of neurons to be updated
 */
fun updateNeurons(neuronList: List<Neuron>) {
    // TODO: Update by priority if priority based update?
    neuronList.forEach(Neuron::updateInputs)
    neuronList.forEach(Neuron::update)
}

/**
 * Returns a reference to the synapse connecting two neurons, or null if there is none.
 *
 * @param src source neuron
 * @param tar target neuron
 * @return synapse from source to target
 */
fun getFreeSynapse(src: Neuron, tar: Neuron): Synapse? = src.fanOut[tar]

/**
 * Convenient access to a list of activations
 */
var List<Neuron?>.activations: List<Double>
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            if (neuron.isClamped) {
                neuron.forceSetActivation(value)
            } else {
                neuron.activation = value
            }
        }
    }

var List<Neuron?>.labels: List<String>
    get() = map { it?.label ?: "" }
    set(values) = values.forEachIndexed { index, label ->
        this[index]?.let { it.label = label }
    }

fun AbstractNeuronCollection.setLabels(labels: List<String>) {
    neuronList.labels = labels
}

var List<Neuron>.auxValues: List<Double>
    get() = map { it.auxValue }
    set(values) = values.forEachIndexed { index, value ->
        this[index].auxValue = value
    }

/**
 * Length in pixels of synapses. See Syanpse.length.
 */
val List<Synapse>.lengths: List<Double>
    get() = map { it.length }

fun getNetworkXStream(): XStream {
    val xstream = getSimbrainXStream()
    xstream.registerConverter(NetworkModelListConverter())
    xstream.registerConverter(DoubleArrayConverter())
    xstream.registerConverter(MatrixConverter())
    return xstream
}

fun networkUpdateAction(description: String, longDescription: String = description, action: () -> Unit) =
    object : NetworkUpdateAction {
        override fun invoke() = action()
        override fun getDescription(): String = description
        override fun getLongDescription(): String = longDescription
    }

/**
 * Layout a neuron group.
 *
 * @param ng reference to the group
 * @param layoutName the type of layout to use: "line" (defaults to horizontal),
 * "vertical line", or "grid".  TODO: Add hex.
 */
fun layoutNeuronGroup(ng: NeuronGroup, layoutName: String) {
    if (layoutName.toLowerCase().contains("line")) {
        if (layoutName.equals("vertical line", ignoreCase = true)) {
            val lineLayout = LineLayout(50.0, LineLayout.LineOrientation.VERTICAL)
            ng.layout = lineLayout
        } else {
            val lineLayout = LineLayout(50.0, LineLayout.LineOrientation.HORIZONTAL)
            ng.layout = lineLayout
        }
    } else if (layoutName.equals("grid", ignoreCase = true)) {
        val gridLayout = GridLayout(50.0, 50.0, Math.sqrt(ng.size().toDouble()).toInt()
        )
        ng.layout = gridLayout
    }
    ng.applyLayout()
}

/**
 * Make a single source -> target neuron connection.
 *
 * @param source the source neuron
 * @param target the target neuron
 */
fun connect(source: Neuron, target: Neuron, value: Double): Synapse {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    source.network.addNetworkModelAsync(synapse)
    return synapse
}

/**
 * Make a single source -> target neuron connection with specified upper and lower bounds for the synapses.
 */
fun connect(source: Neuron, target: Neuron, value: Double, lowerBound: Double, upperBound: Double) {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    synapse.lowerBound = lowerBound
    synapse.upperBound = upperBound
    source.network.addNetworkModelAsync(synapse)
}

/**
 * Connect source to target with a provided learning rule and value.
 *
 * @return the new synapse
 */
fun connect(source: Neuron, target: Neuron, rule: SynapseUpdateRule, value: Double): Synapse? {
    val synapse = Synapse(source, target, rule)
    synapse.forceSetStrength(value)
    source.network.addNetworkModelAsync(synapse)
    return synapse
}

/**
 * Connect input nodes to target nodes with weights initialized to a value.
 */
fun connectAllToAll(source: NeuronGroup, target: NeuronGroup, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach{wt: Synapse -> wt.forceSetStrength(value)}
    return wts
}

fun connectAllToAll(source: NeuronGroup, target: NeuronGroup): List<Synapse> {
    return AllToAll().connectNeurons(source.network, source.neuronList, target.neuronList)
}

/**
 * Connect a source neuron group to a single target neuron
 */
fun connectAllToAll(inputs: NeuronGroup, target: Neuron): List<Synapse> {
    val connector = AllToAll()
    return connector.connectNeurons(inputs.network, inputs.neuronList, listOf(target))
}

/**
 * Connect input nodes to target node with weights initialized to a value.
 */
fun connectAllToAll(source: NeuronGroup, target: Neuron, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach{ wt: Synapse -> wt.forceSetStrength(value) }
    return wts
}

fun Network.addNeurons(numNeurons: Int, template: Neuron.() -> Unit = {}): List<Neuron> {
    val neurons = (0 until numNeurons).map {
        Neuron(this).apply(template)
    }
    addNetworkModelsAsync(neurons)
    return neurons
}

fun Network.addNeuronCollection(numNeurons: Int, template: Neuron.() -> Unit = {}) : NeuronCollection {
    val nc = NeuronCollection(this, addNeurons(numNeurons, template))
    addNetworkModelAsync(nc)
    return nc
}

/**
 * Convenience methods to set parameters for inhibitory methods in a prob. dist
 */
fun ProbabilityDistribution.useInhibitoryParams() {
    when(this) {
        is UniformRealDistribution -> {
            ceil = 0.0
            floor = -1.0
        }
        is NormalDistribution ->   mean = -1.0
        is UniformIntegerDistribution -> {
            ceil = 0
            floor = -1
        }
    }
}

fun List<Synapse>.decayStrengthBasedOnLength(decay: DecayFunction) {
    forEach{ it.decayStrengthBasedOnLength(decay) }
}

fun Synapse.decayStrengthBasedOnLength(decay: DecayFunction) {
    strength *= decay.getScalingFactor(length)
}

/**
 * Return true if the synapse "overlaps" an existing synapse
 */
fun Synapse.overlapsExistingSynapse(): Boolean {
    // For the source neuron, check if there is already a fanOut synapse linking to the same target,
    // and if there is, it is not this synapse
    return this.source.fanOut[target].let { it != null && it != this }
}