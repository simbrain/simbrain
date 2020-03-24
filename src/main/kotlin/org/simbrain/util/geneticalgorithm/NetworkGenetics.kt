package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.clip

class NetworkEnvironmentBuilder {
    var eval: NetworkGenome.() -> Double = { 0.0 }
    var templateNetwork = NetworkGenome(listOf(), listOf(), "")
    var populationSize = 100
    var seed = 1L
}

fun networkEnvironment(init: NetworkEnvironmentBuilder.() -> Unit) = NetworkEnvironmentBuilder().apply(init).run {
    NetworkEnvironment(
            eval = eval,
            initialTemplate = List(populationSize) { templateNetwork },
            config = NetworkConfig(populationSize),
            seed = seed
    )
}

class NetworkEnvironment(eval: NetworkGenome.() -> Double, initialTemplate: List<NetworkGenome>,
                         override val config: NetworkConfig, seed: Long)
    : Environment<NetworkAgent, NetworkGenome>(
        name = "NetworkGenome",
        seed = seed,
        eval = eval
) {

    override val initialPopulation = initialTemplate.map { it.express() }

    override fun NetworkGenome.crossOver(other: NetworkGenome) = copy()

    override fun NetworkGenome.mutate() = copy().apply {
        nodes.forEach { it.mutate() }
        connections.forEach { it.mutate() }
    }

    fun NodeChromosome.mutate() = copy().applyRandomly(0.05) {
        it.copy(genes = genes + NodeGene(Neuron(null)))
    }

    fun ConnectionChromosome.mutate() = ConnectionChromosome(
            genes.map { it.mutate() }
    )

    fun NodeGene.mutate(): NodeGene {
        return copy(prototype = prototype.deepCopy().apply { (updateRule as LinearRule).bias = -1.0 })
    }

    fun ConnectionGene.mutate() = copy(
            prototype = Synapse(prototype).apply { strength = strength.rand(0.1).clip(lowerBound, upperBound) }
    )

    override fun NetworkGenome.express(): NetworkAgent {
        val network = Network()
        nodes.flatMap { it.genes.map { n -> Neuron(network, n.prototype) } }.forEach { network.addLooseNeuron(it) }
        connections.flatMap { it.genes.map { s ->
            Synapse(network.getLooseNeuron(s.source), network.getLooseNeuron(s.target)) }
        }.forEach { network.addLooseSynapse(it) }
        return NetworkAgent(network, this, Double.NaN)
    }

    override fun NetworkAgent.eval() = copy(fitness = genome.eval())

}

data class NetworkConfig(override val populationSize: Int) : EnvironmentConfig

data class NetworkAgent(
        val network: Network,
        override val genome: NetworkGenome,
        override val fitness: Double
) : Agent2<NetworkGenome>()

data class NetworkGenome(
        val nodes: List<NodeChromosome>,
        val connections: List<ConnectionChromosome>,
        override val id: String
) : Genome2()

data class ConnectionChromosome(override val genes: List<ConnectionGene>) : Chromosome2<ConnectionGene>()

data class NodeChromosome(override val genes: List<NodeGene>) : Chromosome2<NodeGene>()

data class ConnectionGene(val source: String, val target: String, val prototype: Synapse) : Gene2()

fun createConnectionChromosome(source: String, target: String, config: Synapse.() -> Unit = { })
    = ConnectionGene(source, target, Synapse(null, null, 1.0).apply(config))

data class NodeGene(val prototype: Neuron) : Gene2()