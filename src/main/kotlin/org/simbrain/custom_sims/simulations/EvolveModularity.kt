package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import java.io.File
import java.util.*

/**
 * TODO
 */
val evolveModularity = newSim {

    val evolutionarySim = evolutionarySimulation {

        val network = Network()

        /**
         * Retina
         */
        val retinaChromosome = chromosome(8) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(it*30,-50) // TODO: Make a grid
                lowerBound = 0.0
                upperBound = 1.0
                isClamped = true
            }
        }

        // TODO: Make this correspond to the layered architecture in Clune
        val nodeChromosome = chromosome(1) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
            }
        }

        val connectionChromosome = chromosome<Synapse, ConnectionGene>()

        onMutate {

            // Local extension function for mutating biases
            fun NodeGene.mutateBias() = mutate {
                updateRule.let {
                    if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }

            // Local extension for mutating connection
            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }

            // Add new nodes
            // if (Random().nextDouble() > .95) {
            //     nodeChromosome.genes.add(nodeGene())
            // }

            nodeChromosome.genes.forEach {
                it.mutateBias()
            }

            // New connections
            val source = retinaChromosome.genes.shuffled().first()
            val target = nodeChromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })

            // Weight mutations
            connectionChromosome.forEach { it.mutateWeight() }
        }

        onBuild { visible ->
            network {
                +retinaChromosome
                +nodeChromosome
                +connectionChromosome
            }
        }

        onEval {

            // TODO: Separate left and right
            val inputData = listOf(
                listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0),
                listOf(1.0,1.0,1.0,0.0,1.0,1.0,1.0,0.0))
            val tarData = listOf(listOf(0.0), listOf(1.0))
            inputData.zip(tarData).map { (i, t) ->
                retinaChromosome.products.activations = i
                network.apply {
                    repeat(3) { bufferedUpdate() }
                }
                t sse nodeChromosome.products.activations
            }.sum()
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 142,0,400,400)
            withGui {
                createControlPanel("Control Panel", 5, 10) {
                    addButton("Pattern 1") {
                        retinaChromosome.products.activations = listOf(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
                        workspace.iterate()
                    }
                    addButton("Pattern 2") {
                        retinaChromosome.products.activations = listOf(1.0,1.0,1.0,0.0,1.0,1.0,1.0,0.0)
                        workspace.iterate()
                    }
                }
            }
            // When run headless store the winning network
            if (desktop == null) {
                workspace.save(File("evolved.zip"))
            }
        }

    }

    val evolution = evaluator(evolutionarySim) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 50 || fitness < .01 }
    }

    workspace.clearWorkspace()

    val generations = evolution.start().onEachGenerationBest { agent, gen ->
        println("Generation ${gen}, Fitness ${agent.fitness}")
    }

    val (winner, fitness) = generations.best
    // println("Winning fitness $fitness after generation ${generations.finalGenerationNumber}")
    winner.visibleBuild().peek()

}

fun main() {
    evolveNetwork.run()
}
