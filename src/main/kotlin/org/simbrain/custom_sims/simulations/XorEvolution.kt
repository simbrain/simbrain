package org.simbrain.custom_sims.simulations

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.geneticalgorithm.*

fun main() {

    val environment = networkEnvironment {
        val network = Network()
        val neuron1 = Neuron(network)
        templateNetwork = NetworkGenome(
                nodes = listOf(NodeChromosome(
                        listOf(NodeGene(neuron1.apply { label = "Neuron1" }), NodeGene(Neuron(network).apply { label =
                                "Neuron2" }))
                )),
                connections = listOf(ConnectionChromosome(
                        listOf(ConnectionGene("Neuron1", "Neuron2", Synapse(null, null, 1.0)))
                )),
                id = "Network1"
        )
        eval = {
            neuron1.activation - 1.0
        }
    }

    environment.runEvolution()
            .upToGeneration(500)
            .untilFitnessScore { it < 0.1 }
            .onEach { println(it.best()) }
            .last()
            .best()

}

