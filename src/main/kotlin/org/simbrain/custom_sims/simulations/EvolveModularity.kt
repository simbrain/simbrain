package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import java.io.File
import java.util.*

/**
 * A partial replication of Clune, Mouret and Lipson 2018, "The evolutionary origins of modularity"
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
                location = point(it * 30, -50)
                isClamped = true
                applyDefaultParams()
            }
        }

        val rightRetina = chromosome(4) {
            nodeGene() {
                updateRule = BinaryRule()
                location = point(150 + (it * 30), -50)
                isClamped = true
                applyDefaultParams()
            }
        }

        val layer1Chromosome = chromosome(8) {
            nodeGene() {
                location = point(20 + (it * 30), 0)
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

        val thresholds = listOf(-2.0,-1.0,0.0,1.0,2.0)
        val weightStrengths = listOf(-2.0,-1.0,1.0,2.0)

        // See https://docs.google.com/document/d/1MnMAvfI49NKmDyIITVJBxckvXHSDR-kPY1p7vE2hwTk/edit
        onMutate {

            // TODO: Clipping

            // TODO
            // Local extension function for mutating biases
            // fun NodeGene.mutateThreshold() = mutate {
            //     updateRule.let {
            //         if (it is BinaryRule) it.threshold = thresholds.shuffled().first()
            //     }
            // }

            // Local extension for mutating connection
            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
                // strength += weightStrengths.shuffled().first()
            }

            // TODO: do it for all nodes
            // layer1Chromosome.genes.forEach {
            //    if Random.nextDouble() > 1/24 {
            //     it.mutateThreshold()
            //     }
            // }

            // Utility to mutate connections
            fun Chromosome<Synapse, ConnectionGene>.mutateConnections(
                sourceLayer: Chromosome<Neuron, NodeGene>,
                targetLayer: Chromosome<Neuron, NodeGene>
            ) {
                val source = sourceLayer.genes.shuffled().first()
                val target = targetLayer.genes.shuffled().first()
                genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5) * 0.2
                })
            }

            // Create connection genes
            if (Random().nextDouble() > .8) {
                when (Random().nextInt(5)) {
                    0 -> connectionChromosome.mutateConnections(leftRetina, layer1Chromosome)
                    1 -> connectionChromosome.mutateConnections(rightRetina, layer1Chromosome)
                    2 -> connectionChromosome.mutateConnections(layer1Chromosome, layer2Chromosome)
                    3 -> connectionChromosome.mutateConnections(layer2Chromosome, layer3Chromosome)
                    4 -> connectionChromosome.mutateConnections(layer3Chromosome, outputChromosome)
                }
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

        // See Clune et. al. Fig2a
        val leftInputs = listOf(
            listOf(1.0, 1.0, 1.0, 1.0),
            listOf(1.0, 1.0, 1.0, 0.0),
            listOf(1.0, 0.0, 1.0, 0.0),
            listOf(1.0, 0.0, 1.0, 1.0),
            listOf(1.0, 0.0, 0.0, 0.0),
            listOf(1.0, 1.0, 0.0, 1.0),
            listOf(0.0, 0.0, 1.0, 0.0),
            listOf(0.0, 1.0, 1.0, 1.0)
        )

        val rightInputs = listOf(
            listOf(1.0, 1.0, 1.0, 1.0),
            listOf(1.0, 1.0, 1.0, 0.0),
            listOf(0.0, 1.0, 0.0, 1.0),
            listOf(0.0, 0.0, 0.0, 1.0),
            listOf(1.0, 0.0, 0.0, 0.0),
            listOf(1.0, 1.0, 0.0, 1.0),
            listOf(0.0, 1.0, 0.0, 0.0),
            listOf(0.0, 1.0, 1.0, 1.0)
        )

        onEval {

            val tarData = listOf(listOf(0.0), listOf(1.0))

            // TODO: See https://linear.app/simbrain/issue/SIM-35/implement-clune-paper

            // TODO: Need functions
            //  - Checks whether left is in leftinputs
            //  - Checks whether right is in rightinputs
            //  - Computes the target on that basis


            // leftInputs.zip(tarData).map { (i, t) ->
            //     leftRetina.products.activations = i
            //     network.apply {
            //         repeat(4) { bufferedUpdate() }
            //     }
            //     t sse outputChromosome.products.activations
            // }.sum()

            0.0 // so compiler will be quiet
        }


        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 170, 0, 400, 400)
            withGui {
                // TODO: Generalize this with a loop
                createControlPanel("Control Panel", 5, 10) {
                    addButton("Random pattern") {
                        rightRetina.products.activations = DoubleArray(4) { Random().nextInt(2).toDouble() }.asList()
                        leftRetina.products.activations = DoubleArray(4) { Random().nextInt(2).toDouble() }.asList()
                        workspace.iterate()
                    }
                    addButton("Left Pattern 1") {
                        leftRetina.products.activations = leftInputs[0]
                        workspace.iterate()
                    }
                    addButton("Right Pattern 2") {
                        rightRetina.products.activations = rightInputs[1]
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
