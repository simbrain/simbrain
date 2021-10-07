package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.bound
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.core.lengths
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import java.io.File
import java.util.*
import kotlin.math.abs

/**
 * Evolve a network. Several fitness functions are included which can be commented on or off.
 */
val evolveNetwork = newSim {

    val evolutionarySimulation = evolutionarySimulation {

        val network = Network()

        /**
         * Testing evolution of nodes with fixed characteristics
         */
        val motivations = chromosome(2) {
            nodeGene() {
                label = "Fixed node ${it+1}"
                location = point(it*100,-50)
                lowerBound = -10.0
                upperBound = 10.0
            }
        }

        val nodeChromosome = chromosome(2) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
            }
        }

        val connectionChromosome = chromosome<Synapse, ConnectionGene>()

        val layoutChromosome = chromosome(1) {
            layoutGene()
        }

        onMutate {

            // Local functions that define how to mutate genes
            fun LayoutGene.mutateParam() = mutate {
                hSpacing += random.nextDouble(-1.0, 1.0)
                vSpacing += random.nextDouble(-1.0, 1.0)
            }

            fun LayoutGene.mutateType() = mutate {
                when (random.nextDouble()) {
                    in 0.0..0.5 -> layout = GridLayout()
                    in 0.5..1.0 -> layout = HexagonalGridLayout()
                    // in 0.1..0.15 -> layout = LineLayout()
                }
            }

            fun NodeGene.mutateBias() = mutate {
                neuronDataHolder.let {
                    if (it is BiasedScalarData) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }

            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }

            // Apply mutations across chromosomes

            // Add nodes
            if (Random().nextDouble() > .95) {
                nodeChromosome.genes.add(nodeGene())
            }

            // // Remove node. Does not work.
            // if (Random().nextDouble() > .95) {
            //     if (nodeChromosome.genes.isNotEmpty()) {
            //         nodeChromosome.genes.removeLast()
            //     }
            // }

            motivations.genes.forEach {
                it.mutateBias()
            }

            nodeChromosome.genes.forEach {
                it.mutateBias()
            }

            // Modify the layout
            layoutChromosome.genes.forEach {
                it.mutateParam()
                it.mutateType()
            }

            // New connections
            val source = nodeChromosome.genes.shuffled().first()
            val target = nodeChromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })

            // Weight mutations
            connectionChromosome.forEach { it.mutateWeight() }
        }

        onBuild { visible ->
            val (layout) = +layoutChromosome
            network {
                +motivations

                (+nodeChromosome).apply {
                    layout.layoutNeurons(this)
                }
                +connectionChromosome
            }
        }

        onEval {

            // Iterate to stabilize network but sometimes fails for small numbers.
            repeat(50) { network.bufferedUpdate() }

            // Comment / Uncomment different choices of fitness function here
            fun fitness() : Double {

                var avgLength = connectionChromosome.products.lengths.average()

                val numWeights = connectionChromosome.products.size

                val avgActivation = nodeChromosome.products.activations.average()
                val totalActivation = nodeChromosome.products.activations.sum()

                // Evolve fixed nodes to have specific activations 2.5 and -3
                val (m1, m2) =  motivations.products
                val m1error = abs(m1.activation - 2.5)
                val m2error = abs(m2.activation + 3)

                // TODO: Normalize errors and provide for weightings
                val numNodesError = abs(nodeChromosome.products.size - 20).toDouble()
                val numWeightsError = abs(numWeights - 40)
                val axonLengthError = abs(avgLength - 250)
                val avgActivationError = abs(avgActivation - 5)
                val totalActivationError = abs(totalActivation - 10)

                // Area in thousands of pixels
                val bounds  = network.looseNeurons.bound
                val size = (bounds.height * bounds.width) / 10_000
                val areaError = abs(size - 10)

                // return m1error + m2error + numNodesError + numWeightsError + axonLengthError +
                //         totalActivationError + areaError

                return numNodesError + axonLengthError + totalActivationError

            }

            fitness()
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 0,0,400,400)

            // When run headless store the winning network
            if (desktop == null) {
                workspace.save(File("evolved.zip"))
            }
        }

    }

    val evolution = evaluator(evolutionarySimulation) {
        populationSize = 100
        eliminationRatio = 0.25
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 1000 || fitness < .1 }
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
