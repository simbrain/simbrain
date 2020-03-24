package org.simbrain.util.geneticalgorithm

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.SigmoidalRule
import org.simbrain.network.util.activations
import org.simbrain.util.sse
import org.simbrain.workspace.Workspace
import kotlin.random.Random
import kotlin.streams.toList

fun main() {

    val trainingSet = mapOf(
            listOf(0.0, 0.0) to listOf(0.0),
            listOf(0.0, 1.0) to listOf(1.0),
            listOf(1.0, 0.0) to listOf(1.0),
            listOf(1.0, 1.0) to listOf(0.0)
    )

    val initial = List(100) { XorGenome() }.map { XorAgent(it) }

    fun eval(net: XorAgent) = trainingSet.map { (input, output) ->
        net.age += 1
        net.inputs.activations = input
        repeat(5) { net.iterate() }
        (output sse net.outputs.activations)
    }.sum() + (net.age * 0.01)

    val result = generateSequence(initial) { population ->
        val best = population.parallelStream()
                .map { it.apply { fitness = eval(it) } }
                .toList()
                .sortedBy { it.fitness }
                .take(50)
                .toList()
        best + best.map { it.xorGenome.copy().apply { mutate() } }.map { XorAgent(it) }
    }.takeWhile { it.first().fitness.isNaN() || it[0].fitness > 0.5 }
            .map { it[0] }
            .onEach { println("[${it.fitness}] ${it.network}") }
            .toList()

    val winner: Network = result.last().network;
    NetworkPanel.showNetwork(winner)

}

class XorAgent(val xorGenome: XorGenome) {

    private val mapping: Map<Int, Neuron>

    var age = 0

    var fitness = Double.NaN

    val inputs get() = listOf(mapping[0], mapping[1])

    val outputs get() = listOf(mapping[2])

    val network = Network().apply {

        mapping = xorGenome.nodeChromosome.map { (k, v) ->
            k to Neuron(this).apply { updateRule = v.updateRule }
        }.also { it.forEach { (_, v) -> addLooseNeuron(v) } }.toMap()

        xorGenome.connectionChromosome.map {
            Synapse(mapping[it.source], mapping[it.target], it.strength)
        }.forEach { addLooseSynapse(it) }
    }

    private val workspace by lazy {
        Workspace().apply { addWorkspaceComponent(NetworkComponent("net", network)) }
    }

    fun iterate() {
        workspace.simpleIterate()
    }

}


class XorGenome(
        val nodeChromosome: HashMap<Int, NodeGene4> = hashMapOf(
                0 to NodeGene4(updateRule = BinaryRule(0.0, 1.0, 0.5), clamped = true),
                1 to NodeGene4(updateRule = BinaryRule(0.0, 1.0, 0.5), clamped = true),
                2 to NodeGene4(updateRule = LinearRule())
        ),
        val connectionChromosome: HashSet<ConnectionGene4> = HashSet()
) {

    var innovationNumber = 3


    fun newConnectionMutation() {
        nodeChromosome.keys.shuffled().let {
            ConnectionGene4(it[0], it[1], Random.nextDouble(-10.0, 10.0))
        }.also { connectionChromosome.add(it) }
    }

    fun newNodeMutation() {
        NodeGene4(updateRule = SigmoidalRule()).also { nodeChromosome[innovationNumber++] = it }
    }

    fun NodeGene4.mutate() {
        if (updateRule is SigmoidalRule) updateRule.bias = updateRule.bias + Random.nextDouble(-0.1, 0.1)
    }

    fun ConnectionGene4.mutate() {
        strength += Random.nextDouble(-0.5, 0.5)
    }

    fun mutate() {
        nodeChromosome.values.shuffled().forEach { it.mutate() }
        connectionChromosome.shuffled().forEach { it.mutate() }
        if (Random.nextDouble() < 0.1) newNodeMutation()
        if (Random.nextDouble() < 0.1) newConnectionMutation()
    }

    fun copy() = XorGenome(
            HashMap(nodeChromosome.map { (k, v) -> k to v.copy() }.toMap()),
            connectionChromosome.map { it.copy() }.toHashSet()
    ).also { it.innovationNumber = this.innovationNumber }

}


data class NodeGene4(val updateRule: NeuronUpdateRule = SigmoidalRule(), val clamped: Boolean = false)

data class ConnectionGene4(val source: Int, val target: Int, var strength: Double)