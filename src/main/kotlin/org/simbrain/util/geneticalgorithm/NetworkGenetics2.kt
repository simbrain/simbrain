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

@NetworkDSLMarker
class NetworkWrapper {

    val network = Network()

    private val geneticsRandom = Random(1L)

    private fun random() = geneticsRandom

    private fun IntRange.random() = this.random(geneticsRandom)

    private fun Double.rand(range: Double) = geneticsRandom.nextDouble(-range, range) + this

    private var nodeGenes = AutoIncrementMap<Neuron>()

    private var connectionGenes = AutoIncrementMap<Synapse>()

    private var combinations = mutableListOf<ConnectionChromosome3>()

    fun <T> T.applyRandomly(probability: Double, block: (T) -> Unit): T {
        return if (geneticsRandom.nextDouble() < probability) {
            apply(block)
        } else {
            this
        }
    }

    private val mutable = mutableListOf<Neuron>()

    private val nodeChromosomes = mutableListOf<NodeChromosome3>()

    fun Neuron.addToNetwork() = also {
        network.addLooseNeuron(it)
        nodeGenes.add(it)
    }

    fun Neuron.toMutable() = also { mutable.add(it) }

    inline fun nodeGene(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).addToNetwork().toMutable()

    fun nodeChromosome() = NodeChromosome3(mutableListOf()).also { nodeChromosomes.add(it) }

    inline fun neuron(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).addToNetwork()

    inline fun neuronGroup(config: NeuronGroupBuilder.() -> Unit) = NeuronGroupBuilder(network)

    fun synapse(source: Neuron, target: Neuron) = Synapse(source, target)

    fun connectionGene(sourceSet: Collection<Neuron>, targetSet: Collection<Neuron>)
            = ConnectionChromosome3(sourceSet, targetSet).also { combinations.add(it) }

    fun connectionGene(sourceSet: NodeChromosome3, targetSet: Collection<Neuron>)
            = ConnectionChromosome3(sourceSet.nodes, targetSet).also { combinations.add(it) }

    fun connectionGene(sourceSet: Collection<Neuron>, targetSet: NodeChromosome3)
            = ConnectionChromosome3(sourceSet, targetSet.nodes).also { combinations.add(it) }

    fun connectionGene(sourceSet: NodeChromosome3, targetSet: NodeChromosome3)
            = ConnectionChromosome3(sourceSet.nodes, targetSet.nodes).also { combinations.add(it) }

    fun copy() = NetworkWrapper().also { new ->
        new.nodeGenes = nodeGenes.copy { neuron -> with(new) { Neuron(network, neuron).addToNetwork() } }
        new.nodeChromosomes.addAll(nodeChromosomes.map {
            NodeChromosome3(it.nodes.map { neuron -> new.nodeGenes[nodeGenes[neuron]] }.toMutableList())
        })
        new.mutable.addAll(mutable.map { neuron -> new.nodeGenes[nodeGenes[neuron]] })
        new.combinations = combinations.map { it.copy { neuron -> new.nodeGenes[nodeGenes[neuron]] } }.toMutableList()
        new.connectionGenes = connectionGenes.copy {
            Synapse(new.nodeGenes[nodeGenes[it.source]], new.nodeGenes[nodeGenes[it.target]])
        }
    }

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

}

fun main() {
    val baseNetwork = NetworkWrapper().apply {
        val source = listOf(neuron(), neuron())
        val hidden = nodeChromosome()
        val target = listOf(neuron())
        connectionGene(source, target)
        connectionGene(source, hidden)
        connectionGene(hidden, target)
    }
    val newNet = baseNetwork.copy().apply { repeat(10) { mutate() } }
    println(newNet)
}

class NodeChromosome3(val nodes: MutableList<Neuron>)

class AutoIncrementMap<V> : AbstractMap<Int, V>() {

    var nextId = 0

    private val map = BiMap<Int, V>()

    override operator fun get(key: Int) = map[key] ?: throw NoSuchElementException()

    fun add(value: V) = map.put(nextId++, value)

    operator fun get(value: V) = map.getInverse(value) ?: throw NoSuchElementException()

    override val entries: Set<Map.Entry<Int, V>>
        get() = map.entries

    fun copy(copier: (V) -> V) = AutoIncrementMap<V>().also {
        map.map { (i, v) -> i to v }.map { (i, v) -> i to copier(v) }.forEach { (i, v) -> it.map[i] = v }
        it.nextId = nextId
    }

}

class ConnectionChromosome3(val sourceSet: Collection<Neuron>, val targetSet: Collection<Neuron>) {

    fun copy(copier: (Neuron) -> Neuron) = ConnectionChromosome3(sourceSet.map(copier), targetSet.map(copier))

}

@NetworkDSLMarker
class NeuronGroupBuilder(val network: Network) {

    val neuronGroup = NeuronGroup(network)

    inline fun neuron(config: Neuron.() -> Unit = { }) = Neuron(network).apply(config).also { neuronGroup.addNeuron(it) }
}