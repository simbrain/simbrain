package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.util.BiMap
import org.simbrain.util.clip
import kotlin.random.Random

@DslMarker
annotation class NetworkDSLMarker

// This may be used by a future NetworkGenome. Or it will become NetworkGenome.

@NetworkDSLMarker
class NetworkWrapper {

    /**
     * Temnplate network used for all genes.  Node and connection genes have references to neurons and synapses
     * which must exist in a network.  This is that network.
     */
    val network = Network()

    private val geneticsRandom = Random(1L)

    private fun random() = geneticsRandom

    private fun IntRange.random() = this.random(geneticsRandom)

    private fun Double.rand(range: Double) = geneticsRandom.nextDouble(-range, range) + this

    /**
     * Associates neurons with integer ids.
     */
    private var nodeGenes = AutoIncrementMap<Neuron>()

    /**
     * Associates synapses with integer ids.
     */
    private var connectionGenes = AutoIncrementMap<Synapse>()

    /**
     * When you have source, hidden, and output layers
     */
    private var combinations = mutableListOf<ConnectionChromosome3>()

    /**
     * The mutable subset of node genes
     */
    private val mutableNodeGenes = mutableListOf<Neuron>()

    /**
     * Chromosomes holding the nodes.  Inputs, hidden, and outputs are
     * node chromosomes.  They are like neuron groups.
     */
    private val nodeChromosomes = mutableListOf<NodeChromosome3>()

    /**
     * Convenient extension to [Neuron] for adding them to a network.
     */
    fun Neuron.addToNetwork() = also {
        network.addLooseNeuron(it)
        nodeGenes.add(it)
    }

    /**
     * Convenient extension to [Synapse] for adding them to a network.
     */
    fun Synapse.addToNetwork() = also {
        network.addLooseSynapse(it)
        connectionGenes.add(it)
    }

    /**
     * Send a neuron to the mutable list.
     */
    fun Neuron.toMutable() = also { mutableNodeGenes.add(it) }

    /**
     * Create a mutable [nodeGene], with a config function.
     */
    inline fun nodeGene(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).addToNetwork().toMutable()

    /**
     * Create a [NodeChromosome3]
     */
    fun nodeChromosome() = NodeChromosome3(mutableListOf()).also { nodeChromosomes.add(it) }

    /**
     * Create an immutable neuron.
     */
    inline fun neuron(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).addToNetwork()

    /**
     * Create a neuron group.
     */
    inline fun neuronGroup(config: NeuronGroupBuilder.() -> Unit) = NeuronGroupBuilder(network)

    /**
     * Create a synapse.
     */
    fun synapse(source: Neuron, target: Neuron) = Synapse(source, target)


    fun createConnectionChromosome(sourceSet: Collection<Neuron>, targetSet: Collection<Neuron>)
            = ConnectionChromosome3(sourceSet, targetSet).also { combinations.add(it) }

    fun createConnectionChromosome(sourceSet: NodeChromosome3, targetSet: Collection<Neuron>)
            = ConnectionChromosome3(sourceSet.nodes, targetSet).also { combinations.add(it) }

    fun createConnectionChromosome(sourceSet: Collection<Neuron>, targetSet: NodeChromosome3)
            = ConnectionChromosome3(sourceSet, targetSet.nodes).also { combinations.add(it) }

    fun createConnectionChromosome(sourceSet: NodeChromosome3, targetSet: NodeChromosome3)
            = ConnectionChromosome3(sourceSet.nodes, targetSet.nodes).also { combinations.add(it) }

    /**
     * Make a copy of a network.
     */
    fun copy() = NetworkWrapper().also { newNet ->
        newNet.nodeGenes = nodeGenes.copy { neuron -> with(newNet) { Neuron(network, neuron).addToNetwork() } }
        newNet.nodeChromosomes.addAll(nodeChromosomes.map {
            NodeChromosome3(it.nodes.map { neuron -> newNet.nodeGenes[nodeGenes[neuron]] }.toMutableList())
        })
        newNet.mutableNodeGenes.addAll(mutableNodeGenes.map { neuron -> newNet.nodeGenes[nodeGenes[neuron]] })
        newNet.combinations = combinations.map { it.copy { neuron -> newNet.nodeGenes[nodeGenes[neuron]] } }.toMutableList()
        newNet.connectionGenes = connectionGenes.copy {
            Synapse(newNet.nodeGenes[nodeGenes[it.source]], newNet.nodeGenes[nodeGenes[it.target]]).addToNetwork();
        }
    }

    /**
     * Mutate the network wrapper.
     */
    fun mutate() {
        connectionGenes.map { (_, synapse) ->
            synapse.strength = synapse.strength.rand(0.1).clip(synapse.lowerBound..synapse.upperBound)
        }
        nodeGenes.applyRandomly(1.0) {
            val neuron = nodeGene()
            if (nodeChromosomes.size > 0) {
                nodeChromosomes.shuffled()[0].nodes.add(neuron)
            }
        }
        connectionGenes.applyRandomly(1.0) {
            if (combinations.size < 1) return@applyRandomly
            val neurons = combinations.shuffled()[0]
            if (neurons.sourceSet.isNotEmpty() && neurons.targetSet.isNotEmpty()) {
                it.add(Synapse(neurons.sourceSet.shuffled()[0], neurons.targetSet.shuffled()[0]))
            }
        }
    }

    /**
     * Applies a provided function with some given probability.
     */
    fun <T> T.applyRandomly(probability: Double, block: (T) -> Unit): T {
        return if (geneticsRandom.nextDouble() < probability) {
            apply(block)
        } else {
            this
        }
    }

    override fun toString(): String {
        return "Combinations:\n${combinations.joinToString("\n")}\n\nNetwork: $network"
    }
}

class NodeChromosome3(val nodes: MutableList<Neuron>)

/**
 * A 1-1 map associating objects with integer ids.  Cf. NEAT "innovation numbers".
 */
class AutoIncrementMap<V> : AbstractMap<Int, V>() {

    /**
     * Track current id.
     */
    var nextId = 0

    /**
     * The 1-1, invertible map from integers to objects.
     */
    private val map = BiMap<Int, V>()

    /**
     * Get the object corresponding to an integer id.
     */
    override operator fun get(key: Int) = map[key] ?: throw NoSuchElementException()

    /**
     * Add an object to the map.
     */
    fun add(value: V) = map.put(nextId++, value)

    /**
     * Get the integer associated with an object.
     */
    operator fun get(value: V) = map.getInverse(value) ?: throw NoSuchElementException()

    override val entries: Set<Map.Entry<Int, V>>
        get() = map.entries

    /**
     * Copy the map using a provided copier.
     */
    fun copy(copier: (V) -> V) = AutoIncrementMap<V>().also {
        map.map { (i, v) -> i to v }.map { (i, v) -> i to copier(v) }.forEach { (i, v) -> it.map[i] = v }
        it.nextId = nextId
    }

}

class ConnectionChromosome3(val sourceSet: Collection<Neuron>, val targetSet: Collection<Neuron>) {

    fun copy(copier: (Neuron) -> Neuron) = ConnectionChromosome3(sourceSet.map(copier), targetSet.map(copier))

    override fun toString(): String {
        return "Source neurons: ${sourceSet.map { it.id }} -> Target neurons: ${targetSet.map { it.id }}"
    }
}

@NetworkDSLMarker
class NeuronGroupBuilder(val network: Network) {

    val neuronGroup = NeuronGroup(network)

    inline fun neuron(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).also { neuronGroup.addNeuron(it) }
}

/**
 * Test main.
 */
fun main() {

    val baseNetwork = NetworkWrapper().apply {
        // Create separate lists for input and output neurons (which don't mutate)
        val inputs = listOf(neuron(), neuron())
        val outputs = listOf(neuron())

        // Hidden neurons are part of a chromosome that mutates
        val hidden = nodeChromosome()

        // Create separate chromosomes for each layer
        createConnectionChromosome(inputs, outputs)
        createConnectionChromosome(inputs, hidden)
        createConnectionChromosome(hidden, outputs)
    }

    // Mutate the network 10 times
    val newNet = baseNetwork.copy().apply { repeat(10) { mutate() } }

    println(newNet)
}
