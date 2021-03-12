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

        // TODO: How to share information between these chromosomes

        /**
         * Retina
         */
        val leftRetina = chromosome(4) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(it*30,-50) // TODO: Make a grid
                lowerBound = 0.0
                upperBound = 1.0
                isClamped = true
            }
        }

        val rightRetina = chromosome(4) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(150 + (it * 30),-50) // TODO: Make a grid
                lowerBound = 0.0
                upperBound = 1.0
                isClamped = true
            }
        }

        val layer1Chromosome = chromosome(8) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
                location = point(it * 30,0)
            }
        }

        val layer2Chromosome = chromosome(4) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
                location = point(50 + (it * 30),50)
            }
        }

        val layer3Chromosome = chromosome(2) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
                location = point(70 + (it * 30),100)
            }
        }

        val outputChromosome = chromosome(1) {
            nodeGene() {
                lowerBound = -10.0
                upperBound = 10.0
                location = point(90,150)
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

            layer1Chromosome.genes.forEach {
                it.mutateBias()
            }

            // Left to layer 1
            var source = leftRetina.genes.shuffled().first()
            var target = layer1Chromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
            // Right to layer 1
            source = rightRetina.genes.shuffled().first()
            target = layer1Chromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
            // Layer 1 to Layer 2
            source = layer1Chromosome.genes.shuffled().first()
            target = layer2Chromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
            // Layer 2 to Layer 3
            source = layer2Chromosome.genes.shuffled().first()
            target = layer3Chromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
            // Layer 3 to Layer output
            source = layer3Chromosome.genes.shuffled().first()
            target = outputChromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })

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

            // TODO: Separate left and right
            val inputData = listOf(
                listOf(1.0,1.0,1.0,1.0),
                listOf(1.0,1.0,1.0,0.0))
            val tarData = listOf(listOf(0.0), listOf(1.0))
            inputData.zip(tarData).map { (i, t) ->
                leftRetina.products.activations = i
                network.apply {
                    repeat(3) { bufferedUpdate() }
                }
                t sse layer1Chromosome.products.activations
            }.sum()
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 142,0,400,400)
            withGui {
                createControlPanel("Control Panel", 5, 10) {
                    addButton("Pattern 1") {
                        leftRetina.products.activations = listOf(1.0,1.0,1.0,1.0)
                        rightRetina.products.activations = listOf(1.0,0.0,1.0,1.0)
                        workspace.iterate()
                    }
                    addButton("Pattern 2") {
                        leftRetina.products.activations = listOf(1.0,1.0,1.0,0.0)
                        rightRetina.products.activations = listOf(0.0,1.0,1.0,1.0)
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
