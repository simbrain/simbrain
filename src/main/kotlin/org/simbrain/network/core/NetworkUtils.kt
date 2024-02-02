package org.simbrain.network.core

import com.thoughtworks.xstream.XStream
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.groups.*
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import java.awt.geom.Point2D

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
fun getSynapse(src: Neuron, tar: Neuron): Synapse? = src.fanOut[tar]

/**
 * Returns a network model with a matching label.  If more than one
 * model has a matching label, the first found is returned.
 */
inline fun <reified T: NetworkModel> Network.getModelByLabel(label: String): T = getModels<T>().first {
    it.label.equals(label, ignoreCase = true)
}

/**
 * Version of getModelByLabel that works in Java.
 */
fun <T: NetworkModel> Network.getModelByLabel(clazz: Class<T>, label: String): T = getModels(clazz).first {
    it.label.equals(label, ignoreCase = true)
}

/**
 * Unlike other network models, neurons could be in a hierarchy, so we need to search the flattened list.
 */
fun Network.getNeuronByLabel(label: String): Neuron = flatNeuronList.first {
    it.label.equals(label, ignoreCase = true)
}

/**
 * Returns a network model with a matching id.  If more than one
 * model has a matching id, the first found is returned.
 */
inline fun <reified T: NetworkModel> Network.getModelById(id: String): T = getModels<T>().first {
    it.id.equals(id, ignoreCase = true)
}

/**
 * Version of getModelById that works in Java.
 */
fun <T: NetworkModel> Network.getModelById(clazz: Class<T>, id: String): T = getModels(clazz).first {
    it.id.equals(id, ignoreCase = true)
}

/**
 * Convenient access to a list of activations
 */
var List<Neuron?>.activations: List<Double>
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            if (neuron.clamped) {
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
 * Length in pixels of synapses. See Synapse.length.
 */
val List<Synapse>.lengths: List<Double>
    get() = map { it.length }

fun getNetworkXStream(): XStream {
    val xstream = getSimbrainXStream()
    xstream.registerConverter(NetworkModelListConverter())
    return xstream
}

fun networkUpdateAction(description: String, longDescription: String = description, action: () -> Unit) =
    object : NetworkUpdateAction {
        override fun invoke() = action()
        override fun getDescription(): String = description
        override fun getLongDescription(): String = longDescription
    }

@JvmOverloads
fun connect(source: Neuron, target: Neuron, value: Double, lowerBound: Double = Synapse.DEFAULT_LOWER_BOUND, upperBound: Double = Synapse.DEFAULT_UPPER_BOUND): Synapse {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    synapse.lowerBound = lowerBound
    synapse.upperBound = upperBound
    source.network.addNetworkModelAsync(synapse)
    return synapse
}

fun Network.connect(source: List<Neuron>, target: List<Neuron>, connectionStrategy: ConnectionStrategy): List<Synapse> {
    return connectionStrategy.connectNeurons(this, source, target)
}

fun Network.connect(source: AbstractNeuronCollection, target: AbstractNeuronCollection, connector: ConnectionStrategy): List<Synapse?> {
    return connector.connectNeurons(this, source.neuronList, target.neuronList)
}

/**
 * Connect input nodes to target nodes with weights initialized to a value.
 */
fun connectAllToAll(source: AbstractNeuronCollection, target: AbstractNeuronCollection, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach{ it.forceSetStrength(value) }
    return wts
}

fun connectAllToAll(source: AbstractNeuronCollection, target: AbstractNeuronCollection): List<Synapse> {
    return AllToAll().connectNeurons(source.network, source.neuronList, target.neuronList)
}

/**
 * Connect a source neuron group to a single target neuron
 */
fun connectAllToAll(inputs: AbstractNeuronCollection, target: Neuron): List<Synapse> {
    val connector = AllToAll()
    return connector.connectNeurons(inputs.network, inputs.neuronList, listOf(target))
}

/**
 * Connect input nodes to target node with weights initialized to a value.
 */
fun connectAllToAll(source: AbstractNeuronCollection, target: Neuron, value: Double): List<Synapse> {
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

fun Network.addNeuron(block: Neuron.() -> Unit = { }) = Neuron(this)
    .apply(this::addNetworkModelAsync)
    .also(block)

@JvmOverloads
fun Network.addNeuron(x: Int, y: Int, block: Neuron.() -> Unit = { }) = addNeuron(block)
    .also{ it.location = point(x,y) }

fun Network.addSynapse(source: Neuron, target: Neuron, block: Synapse.() -> Unit = { }) = Synapse(source, target)
    .apply(block)
    .also(this::addNetworkModelAsync)

fun Network.addNeuronGroup(count: Int, location: Point2D? = null, template: Neuron.() -> Unit = { }): NeuronGroup {
    return NeuronGroup(this, List(count) {
        Neuron(this).apply(template)
    }).also {
        addNetworkModelAsync(it)
        if (location != null) {
            val (x, y) = location
            it.setLocation(x, y)
        }
    }
}

@JvmOverloads
fun Network.addNeuronGroup(x: Double, y: Double, numNeurons: Int, rule: NeuronUpdateRule<*, *> = LinearRule()):
        NeuronGroup {
    val ng = NeuronGroup(this, numNeurons)
    ng.setUpdateRule(rule)
    addNetworkModelAsync(ng)
    ng.setLocation(x, y)
    return ng
}

fun Network.addNeuronCollectionAsync(numNeurons: Int, template: Neuron.() -> Unit = {}) : NeuronCollection {
    val nc = NeuronCollection(this, addNeurons(numNeurons, template))
    addNetworkModelAsync(nc)
    return nc
}

suspend fun Network.addNeuronCollection(numNeurons: Int, template: Neuron.() -> Unit = {}) : NeuronCollection {
    val nc = NeuronCollection(this, addNeurons(numNeurons, template))
    addNetworkModel(nc)
    return nc
}

/**
 * Add a synapse group between a source and target neuron group
 *
 * @return the new synapse group
 */
fun Network.addSynapseGroup(source: NeuronGroup, target: NeuronGroup): SynapseGroup {
    val sg = SynapseGroup(source, target)
    addNetworkModelAsync(sg)
    return sg
}

fun Collection<Synapse>.decayStrengthBasedOnLength(decay: DecayFunction) {
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

@JvmName("clampSynapses")
fun Collection<Synapse>.clamp(clamped: Boolean) {
    forEach { it.isFrozen = clamped }
}

@JvmName("clampNeurons")
fun Collection<Neuron>.clamp(clamped: Boolean) {
    forEach { it.clamped = clamped }
}

fun Neuron.randomizeBias() {
    dataHolder.let {
        if (it is BiasedScalarData) {
            it.bias = network.biasesRandomizer.sampleDouble()
        }
    }
}

fun NeuronArray.randomizeBiases() {
    dataHolder.let {
        if (it is BiasedMatrixData) {
            for (i in 0 until it.biases.nrow()) {
                it.biases.set(i, 0, network.biasesRandomizer.sampleDouble())
            }
            events.updated.fireAndBlock()
        }
    }
}

fun List<Synapse>.percentExcitatory() = count { it.strength > 0.0 } / size.toDouble() * 100

fun List<Neuron>.getEnergy() = (this cartesianProduct this)
    .mapNotNull { (a, b) -> getSynapse(a, b) }
    .sumOf { it.strength * it.source.activation * it.target.activation } * -0.5
