package org.simbrain.network.core

import com.thoughtworks.xstream.XStream
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.gui.dialogs.NetworkPreferences.biasesRandomizer
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.cartesianProduct
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.distanceTo
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.point
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix

/**
 * Provides an ordering on [NetworkModels] so that the networks are updated and rebuilt in a proper order, for example
 * with neurons created before synapses (which refer to neurons).
 */
fun updatingOrder(obj: NetworkModel): Int = when (obj) {
    is Neuron -> 10
    is NeuronCollection -> 20
    is NeuronArray -> 40
    is TensorLayer -> 45
    is Connector -> 50
    is FlattenConnector -> 52
    is TensorConnector -> 55
    is SynapseGroup -> 60
    is Subnetwork -> 70
    is Synapse -> 80
    is SupervisedModel -> 90
    else -> 55
}

/**
 * Convenience method for asynchronously updating a set of neurons, by calling each neuron's update function (which
 * sets a buffer), and then setting each neuron's activation to the buffer state.
 *
 * @param neuronList the list of neurons to be updated
 */
context(Network)
fun updateNeurons(neuronList: List<Neuron>) {
    // TODO: Update by priority if priority based update?
    neuronList.forEach { it.accumulateInputs() }
    neuronList.forEach { it.update() }
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
 * Builds a weight matrix representing connection strengths from a provided set of source neurons
 * to a set of target neurons. The weight matrix is in "source-target" format (weight i,j represents a weight from node i to node j).
 * For nonexistent connections, 0 is used.
 * The weight can be transposed for target-source format.
 */
fun getWeightMatrix(sourceNeuronList: List<Neuron>, targetNeuronList: List<Neuron>): Matrix = Matrix.of(
    sourceNeuronList.mapIndexed { j, s ->
        targetNeuronList.mapIndexed { i, t ->
            s.fanOut[t]?.strength ?: 0.0
        }.toDoubleArray()
    }.toTypedArray()
)

/**
 * Returns a network model with a matching label.  If more than one
 * model has a matching label, the first found is returned.
 */
inline fun <reified T: NetworkModel> Network.getModelByLabel(label: String): T = getModels<T>().firstOrNull {
    it.label.equals(label, ignoreCase = true)
} ?: throw NoSuchElementException("No model found with label $label of type ${T::class.simpleName}")

/**
 * Version of getModelByLabel that works in Java.
 */
fun <T: NetworkModel> Network.getModelByLabel(clazz: Class<T>, label: String): T = getModels(clazz).firstOrNull {
    it.label.equals(label, ignoreCase = true)
} ?: throw NoSuchElementException("No model found with label $label")

/**
 * Unlike other network models, neurons could be in a hierarchy, so we need to search the flattened list.
 */
fun Network.getNeuronByLabel(label: String): Neuron = flatNeuronList.firstOrNull {
    it.label.equals(label, ignoreCase = true)
} ?: throw NoSuchElementException("No neuron found with label $label")

/**
 * Returns a network model with a matching id.  If more than one
 * model has a matching id, the first found is returned.
 */
inline fun <reified T: NetworkModel> Network.getModelById(id: String): T = getModels<T>().firstOrNull {
    it.id.equals(id, ignoreCase = true)
} ?: throw NoSuchElementException("No model found with id $id")

/**
 * Version of getModelById that works in Java.
 */
fun <T: NetworkModel> Network.getModelById(clazz: Class<T>, id: String): T = getModels(clazz).firstOrNull {
    it.id.equals(id, ignoreCase = true)
} ?: throw NoSuchElementException("No model found with id $id")

/**
 * Convenient access to a list of activations
 */
var List<Neuron?>.activations: List<Double>
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            neuron.activation = value
        }
    }

var List<Neuron?>.labels: List<String>
    get() = map { it?.label ?: "" }
    set(values) = values.forEachIndexed { index, label ->
        this[index]?.let { it.label = label }
    }

fun NeuronCollection.setLabels(labels: List<String>) {
    neuronList.labels = labels
}

var List<Neuron>.auxValues: List<Double>
    get() = map { it.auxValue }
    set(values) = values.forEachIndexed { index, value ->
        this[index].auxValue = value
    }

/**
 * Length in pixels of synapses, i.e. distance in pixels between connected nodes. See [Synapse.length].
 */
val List<Synapse>.lengths: List<Double>
    get() = map { it.length }


fun Neuron.totalFanInStrength(): Double {
    return this.fanIn.sumOf { s -> s.strength }
}

fun List<Neuron>.totalFanInStrength(): Double {
    return this.sumOf{n -> n.totalFanInStrength()}
}

fun getNetworkXStream(): XStream {
    val xstream = getSimbrainXStream()
    xstream.registerConverter(NetworkModelListConverter())
    return xstream
}

context(Network)
@JvmOverloads
fun connect(source: Neuron, target: Neuron, value: Double, lowerBound: Double = Synapse.DEFAULT_LOWER_BOUND, upperBound: Double = Synapse.DEFAULT_UPPER_BOUND): Synapse {
    val synapse = Synapse(source, target)
    synapse.forceSetStrength(value)
    synapse.lowerBound = lowerBound
    synapse.upperBound = upperBound
    addNetworkModelAsync(synapse)
    return synapse
}

fun Network.connect(source: List<Neuron>, target: List<Neuron>, connectionStrategy: ConnectionStrategy): List<Synapse> {
    return connectionStrategy.connectNeurons(source, target).also { it.addToNetworkAsync() }
}

fun Network.connect(source: NeuronCollection, target: NeuronCollection, connector: ConnectionStrategy): List<Synapse?> {
    return connector.connectNeurons(source.neuronList, target.neuronList).also { it.addToNetworkAsync() }
}

/**
 * Connect input nodes to target nodes with weights initialized to a value.
 */
fun Network.connectAllToAll(source: NeuronCollection, target: NeuronCollection, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach{ it.forceSetStrength(value) }
    return wts
}

fun Network.connectAllToAll(source: NeuronCollection, target: NeuronCollection): List<Synapse> {
    return AllToAll().connectNeurons(source.neuronList, target.neuronList).also { it.addToNetworkAsync() }
}

/**
 * Connect a source neuron group to a single target neuron
 */
fun Network.connectAllToAll(inputs: NeuronCollection, target: Neuron): List<Synapse> {
    val connector = AllToAll()
    return connector.connectNeurons(inputs.neuronList, listOf(target)).also { it.addToNetworkAsync() }
}

/**
 * Connect input nodes to target node with weights initialized to a value.
 */
fun Network.connectAllToAll(source: NeuronCollection, target: Neuron, value: Double): List<Synapse> {
    val wts = connectAllToAll(source, target)
    wts.forEach{ wt: Synapse -> wt.forceSetStrength(value) }
    return wts
}

suspend fun Network.addNeurons(numNeurons: Int, template: suspend Neuron.() -> Unit = {}): List<Neuron> {
    val neurons = (0 until numNeurons).map {
        Neuron().apply { template() }
    }
    addNetworkModels(neurons)
    return neurons
}

suspend fun Network.addNeuron(usePlacementManager: Boolean = false, block: Neuron.() -> Unit = { }) = Neuron()
    .also(block)
    .also { addNetworkModel(it, usePlacementManager) }

suspend fun Network.addNeuron(x: Int, y: Int, usePlacementManager: Boolean = false, block: Neuron.() -> Unit = { }) = addNeuron(usePlacementManager, block)
    .also{ it.location = point(x,y) }

suspend fun Network.addSynapse(source: Neuron, target: Neuron, block: Synapse.() -> Unit = { }) =
    Synapse(source, target)
        .apply(block)
        .also { addNetworkModel(it) }

fun Network.addSynapseAsync(source: Neuron, target: Neuron, block: Synapse.() -> Unit = { }) = Synapse(source, target)
    .apply(block)
    .also(this::addNetworkModelAsync)

suspend fun Network.addNeuronCollection(numNeurons: Int, template: suspend Neuron.() -> Unit = {}) : NeuronCollection {
    val nc = NeuronCollection(addNeurons(numNeurons, template))
    addNetworkModel(nc)
    return nc
}

/**
 * Add a synapse group between a source and target neuron collection.
 *
 * @return the new synapse group
 */
fun Network.addSynapseGroup(source: NeuronCollection, target: NeuronCollection): SynapseGroup {
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
 * Create and add a [ConvolutionalNeuralNetwork] wrapper around an existing unowned CNN pipeline.
 *
 * This helper is intended for scripting and simulations to avoid forgetting the add step.
 */
fun Network.addConvolutionalNeuralNetwork(
    inputTensorLayer: TensorLayer,
    outputArray: NeuronArray,
    block: ConvolutionalNeuralNetwork.() -> Unit = { }
): ConvolutionalNeuralNetwork {
    return ConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        .apply(block)
        .also(::addNetworkModelAsync)
}

/**
 * Return true if [target] can be reached from this layer by following outgoing [WeightMatrix]
 * connectors. Used to keep CNN pipeline discovery on the pipeline when side branches (e.g. probe
 * readouts) are attached to pipeline layers.
 */
fun Layer.reachesThroughWeightMatrices(target: Layer, visited: MutableSet<Layer> = mutableSetOf()): Boolean {
    if (this === target) return true
    if (!visited.add(this)) return false
    return outgoingConnectors
        .filterIsInstance<WeightMatrix>()
        .any { it.target.reachesThroughWeightMatrices(target, visited) }
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
    forEach { it.clamped = clamped }
}

@JvmName("clampNeurons")
fun Collection<Neuron>.clamp(clamped: Boolean) {
    forEach { it.clamped = clamped }
}

fun Neuron.randomizeBias(randomizer: ProbabilityDistribution? = null) {
    bias = (randomizer ?: biasesRandomizer).sampleDouble()
}

fun NeuronArray.randomizeBiases(randomizer: ProbabilityDistribution? = null) {
    for (i in 0 until biases.nrow()) {
        biases.set(i, 0, (randomizer ?: biasesRandomizer).sampleDouble())
    }
    events.updated.fire()
}

fun List<Synapse>.percentExcitatory() = count { it.strength > 0.0 } / size.toDouble() * 100

fun List<Neuron>.getEnergy() = ((this cartesianProduct this)
    .mapNotNull { (a, b) -> getSynapse(a, b) }
    .sumOf { it.strength * it.source.activation * it.target.activation } * -0.5)
    .let { if (it == -0.0) 0.0 else it }

/**
 * Sort a list of models left to right and top to bottom
 * Example:
 *   1 2 3
 *     4
 *       5
 *   6 7
 */
fun <T : LocatableModel> List<T>.sortLeftRightTopBottom() = sortedBy { it.location.x }.sortedBy { it.location.y }

/**
 * Calculates the Euclidean distance between two neurons' positions in coordinate space.
 *
 * @param n1 The first neuron.
 * @param n2 The second neuron.
 */
fun getEuclideanDist(n1: Neuron, n2: Neuron): Double {
    return n1.location distanceTo n2.location
}

suspend fun NetworkModel.addToNetwork(network: Network, usePlacementManager: Boolean = true, useAutoAssignId: Boolean = true) = network.addNetworkModel(this, usePlacementManager, useAutoAssignId)
fun NetworkModel.addToNetworkAsync(network: Network, usePlacementManager: Boolean = true, useAutoAssignId: Boolean = true) = network.addNetworkModelAsync(this, usePlacementManager, useAutoAssignId)
suspend fun List<NetworkModel>.addToNetwork(network: Network, usePlacementManager: Boolean = true, useAutoAssignId: Boolean = true) = network.addNetworkModels(this, usePlacementManager, useAutoAssignId)
fun List<NetworkModel>.addToNetworkAsync(network: Network, usePlacementManager: Boolean = true, useAutoAssignId: Boolean = true) = network.addNetworkModelsAsync(this, usePlacementManager, useAutoAssignId)

context(Network) suspend fun <T: NetworkModel> T.addToNetwork(): T {
    addNetworkModel(this)
    return this
}
context(Network) fun NetworkModel.addToNetworkAsync() = addNetworkModelAsync(this)
context(Network) suspend fun <T: NetworkModel> List<T>.addToNetwork(): List<T> {
    addNetworkModels(this)
    return this
}
context(Network) fun List<NetworkModel>.addToNetworkAsync() = addNetworkModelsAsync(this)

/**
 * Returns the neuron in the provided list with the greatest net input or
 * activation (or a randomly chosen neuron among those that "win").
 *
 * @param useActivations if true, use activations instead of net input to determine winner
 * @return the neuron with the highest net input
 */
fun getWinner(neuronList: List<Neuron>, useActivations: Boolean = false): Neuron {
    if (neuronList.isEmpty()) {
        throw IllegalArgumentException("There are no winners in an empty neuron list")
    }
    val winners: MutableList<Neuron> = ArrayList()
    var winner = neuronList[0]
    winners.add(winner)
    for (n in neuronList) {
        val winnerVal = if (useActivations) winner.activation else winner.weightedInputs
        val value = if (useActivations) n.activation else n.weightedInputs
        if (value == winnerVal) {
            winners.add(n)
        } else if (value > winnerVal) {
            winners.clear()
            winner = n
            winners.add(n)
        }
    }
    return if (winners.size == 1) {
        winner
    } else {
        winners[kotlin.random.Random.nextInt(winners.size)]
    }
}

suspend fun Network.createLayeredFreeNeurons(topology: List<Int>, _layerNames: List<String>? = null, alignment: Alignment = Alignment.VERTICAL) {

    val layerNames = _layerNames ?: topology.indices.map {
        val hiddenName = if (topology.size > 3) "Hidden $it" else "Hidden"
        if (it == 0) "Input" else if (it == topology.lastIndex) "Output" else hiddenName
    }

    val direction = if (alignment == Alignment.VERTICAL) Direction.NORTH else Direction.EAST
    val layers = (topology zip layerNames).map { (size, name) ->
        addNeuronCollection(size).apply {
            label = name
            if (alignment == Alignment.HORIZONTAL) {
                layout = LineLayout(40.0, LineLayout.LineOrientation.VERTICAL)
                applyLayout()
            }
        }
    }
    layers.zipWithNext().forEach { (source, target) ->
        val synapseGroup = SynapseGroup(source, target)
        addNetworkModelAsync(synapseGroup)
    }
    layers.zipWithNext().forEach { (source, target) ->
        alignNetworkModels(source, target, alignment)
        offsetNeuronCollections(source, target, direction,150.0)
    }
}
