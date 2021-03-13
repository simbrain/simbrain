package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
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

        fun Neuron.applyDefaultParams() {
            lowerBound = 0.0
            upperBound = 1.0
        }

        /**
         * Retina
         */
        val leftRetina = chromosome(4) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(it * 30, -50) // TODO: Make a grid
                isClamped = true
                applyDefaultParams()
            }
        }

        val rightRetina = chromosome(4) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(150 + (it * 30), -50) // TODO: Make a grid
                isClamped = true
                applyDefaultParams()
            }
        }

        val layer1Chromosome = chromosome(8) {
            nodeGene() {
                location = point(it * 30, 0)
                applyDefaultParams()
            }
        }

        val layer2Chromosome = chromosome(4) {
            nodeGene() {
                location = point(50 + (it * 30), 50)
                applyDefaultParams()
            }
        }

        val layer3Chromosome = chromosome(2) {
            nodeGene() {
                location = point(70 + (it * 30), 100)
                applyDefaultParams()
            }
        }

        val outputChromosome = chromosome(1) {
            nodeGene() {
                location = point(90, 150)
                applyDefaultParams()
            }
        }


        // Utility to create connections
        fun createInitialConnection(
            sourceLayer: Chromosome<Neuron, NodeGene>,
            targetLayer: Chromosome<Neuron, NodeGene>,
        ): ConnectionGene {
            val source = sourceLayer.genes.shuffled().first()
            val target = targetLayer.genes.shuffled().first()
            return connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            }
        }

        val connectionChromosome = chromosome<Synapse, ConnectionGene>() {
            repeat(4) { add(createInitialConnection(leftRetina, layer1Chromosome)) }
            repeat(4) { add(createInitialConnection(rightRetina, layer1Chromosome)) }
            repeat(4) { add(createInitialConnection(layer1Chromosome, layer2Chromosome)) }
            repeat(3) { add(createInitialConnection(layer2Chromosome, layer3Chromosome)) }
            repeat(2) { add(createInitialConnection(layer3Chromosome, outputChromosome)) }
        }

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

            layer1Chromosome.genes.forEach {
                it.mutateBias()
            }

            // Utility to mutate connections
            fun Chromosome<Synapse, ConnectionGene>.mutateConnections(
                sourceLayer: Chromosome<Neuron, NodeGene>,
                targetLayer: Chromosome<Neuron, NodeGene>,
                creationProbability: Double = .5
            ) {
                val source = sourceLayer.genes.shuffled().first()
                val target = targetLayer.genes.shuffled().first()
                if (Random().nextDouble() <= creationProbability) {
                    genes.add(connectionGene(source, target) {
                        strength = (Random().nextDouble() - 0.5) * 0.2
                    })
                }
            }

            // Create connection geness
            with(connectionChromosome) {
                mutateConnections(leftRetina, layer1Chromosome)
                mutateConnections(rightRetina, layer1Chromosome)
                mutateConnections(layer1Chromosome, layer2Chromosome)
                mutateConnections(layer2Chromosome, layer3Chromosome)
                mutateConnections(layer3Chromosome, outputChromosome)
            }

            // Weight mutations
            connectionChromosome.forEach { it.mutateWeight() }
        }

        onBuild { visible ->
            network {
                +leftRetina
                +rightRetina
                +layer1Chromosome
                +layer2Chromosome
                +layer3Chromosome
                +outputChromosome
                +connectionChromosome
            }
        }

        onEval {

            val inputData = listOf(
                listOf(1.0, 1.0, 1.0, 1.0),
                listOf(1.0, 1.0, 1.0, 0.0)
            )
            val tarData = listOf(listOf(0.0), listOf(1.0))
            inputData.zip(tarData).map { (i, t) ->
                leftRetina.products.activations = i
                network.apply {
                    repeat(4) { bufferedUpdate() }
                }
                t sse outputChromosome.products.activations
            }.sum()
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 142, 0, 400, 400)
            withGui {
                createControlPanel("Control Panel", 5, 10) {
                    addButton("Pattern 1") {
                        leftRetina.products.activations = listOf(1.0, 1.0, 1.0, 1.0)
                        rightRetina.products.activations = listOf(1.0, 0.0, 1.0, 1.0)
                        workspace.iterate()
                    }
                    addButton("Pattern 2") {
                        leftRetina.products.activations = listOf(1.0, 1.0, 1.0, 0.0)
                        rightRetina.products.activations = listOf(0.0, 1.0, 1.0, 1.0)
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
