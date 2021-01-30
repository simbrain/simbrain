package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.bound
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.network.util.lengths
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import java.io.File
import java.util.*
import kotlin.math.abs

/**
 * Evolve a network. Several fitness functions are included which can be commented on or off.
 */
val evolveNetwork = newSim {

    val environmentBuilder = environmentBuilder {

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

        val nodeChromosome = chromosome(25) {
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
                    in 0.0..0.05 -> layout = GridLayout()
                    in 0.05..0.1 -> layout = HexagonalGridLayout()
                    in 0.1..0.15 -> layout = LineLayout()
                }
            }

            fun NodeGene.mutateBias() = mutate {
                updateRule.let {
                    if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }

            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }

            // Apply mutations across chromosomes

            // New nodes
            if (Random().nextDouble() > .95) {
                nodeChromosome.genes.add(nodeGene())
            }

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

        onEval {

            // Iterate to stabilize network but sometimes fails for small numbers.
            repeat(50) { network.bufferedUpdate() }

            // Comment / Uncomment different choices of fitness function here
            fun fitness() : Double {

                val bounds  = network.looseNeurons.bound

                var avgLength = connectionChromosome.products.lengths.average()

                val numWeights = connectionChromosome.products.size

                val avgActivation = nodeChromosome.products.activations.average()
                val totalActivation = nodeChromosome.products.activations.sum()

                // Evolve fixed nodes to have specific activations 2.5 and -3
                val (m1, m2) =  motivations.products
                val m1error = abs(m1.activation - 2.5)
                val m2error = abs(m2.activation + 3)

                // val avgActivationError = abs(avgActivation - 5)
                val totalActivationError = abs(totalActivation - 10)
                val lengthError = abs(avgLength - 50)
                val numWeightsError = abs(numWeights - 100)
                val networkSize = bounds.height * bounds.width / 10_000

                // TODO: Normalize errors
                return networkSize - (lengthError + numWeightsError +
                        totalActivationError + m1error + m2error)

            }
            val result = fitness()
            result
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 0,0,400,400)

            // When run headless store the winning network
            if (desktop == null) {
                workspace.save(File("evolved.zip"))
            }
        }

        onBuild { pretty ->
            val (layout) = +layoutChromosome
            network {
                +motivations

                (+nodeChromosome).apply {
                    layout.layoutNeurons(this)
                }
                +connectionChromosome
            }
        }

    }

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MAXIMIZE_FITNESS
        runUntil { generation == 250 || fitness > 200 }
    }

    workspace.clearWorkspace()

    val generations = evolution.start().onEachGenerationBest { agent, gen ->
        println("Generation ${gen}, Fitness ${agent.fitness}")
    }

    val (winner, fitness) = generations.best
    println("Winning fitness $fitness after generation ${generations.finalGenerationNumber}")
    winner.prettyBuild().peek()

}

fun main() {
    evolveNetwork.run()
}
