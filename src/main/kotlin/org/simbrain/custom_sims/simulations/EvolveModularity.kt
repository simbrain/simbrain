package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
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
        val retina = chromosome(8) {
            nodeGene() {
                // TODO: Binary rule
                location = point(it*30,-50) // TODO: Make a grid
                lowerBound = 0.0
                upperBound = 1.0
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

            retina.genes.forEach {
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
            val source = retina.genes.shuffled().first()
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
                +retina

                (+nodeChromosome).apply {
                    layout.layoutNeurons(this)
                }
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
                retina.products.activations = i
                network.apply {
                    repeat(3) { bufferedUpdate() }
                }
                t sse nodeChromosome.products.activations
            }.sum()
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

    val evolution = evaluator(evolutionarySim) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 250 || fitness < .01 }
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
