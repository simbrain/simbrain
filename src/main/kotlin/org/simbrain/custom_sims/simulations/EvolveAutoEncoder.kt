package org.simbrain.custom_sims.simulations


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import java.util.*

/**
 * Evolve a network which associates input vectors with the same output vectors through a smaller hidden layer.
 */
val evolveAutoAssociator = newSim {

    val mainScope = MainScope()

    val evolutionarySimulation = evolutionarySimulation {

        val network = Network()

        // Size of input and output layers
        val size = 8

        // Play with hidden layer size to study compressibility of representations
        val hiddenSize = 4

        val inputs = chromosome(size) { index ->
            nodeGene {
                isClamped = true
                // label = "Input ${index + 1}"
            }
        }

        val hiddenNodes = chromosome(hiddenSize) { nodeGene() }

        val outputs = chromosome(size) { index ->
            nodeGene {
                // label = "Output ${index + 1}"
            }
        }

        val connections = chromosome<Synapse, ConnectionGene>()

        onMutate {

            fun NodeGene.mutateBias() = mutate {
                updateRule.let {
                    if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }

            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }

            hiddenNodes.forEach { it.mutateBias() }

            connections.forEach { it.mutateWeight() }

            // Create synapses
            val (source, target) = if (Random().nextBoolean()) {
                // Input to hidden
                val source = inputs.selectRandom()
                val target = hiddenNodes.selectRandom()
                Pair(source, target)
            } else {
                // Hidden to output
                val source = hiddenNodes.selectRandom()
                val target = outputs.selectRandom()
                Pair(source, target)
            }
            // Can add conditions for recurrent connections
            // e.g. source = node.genes, target = node.genes.

            connections.add(
                connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5) * 0.2
                }
            )
        }

        onEval {

            // Evaluate error by setting inputs to random numbers treating error as difference between input and
            // output activations
            val iterations = 10
            (0..iterations).map {

                // Set outputs in an odd/even pattern, e.g. (.5,.1,.5,.1...)
                // val even = (Random().nextDouble() - 0.5) * 2
                // val odd = (Random().nextDouble() - 0.5) * 2
                // inputs.products.activations = inputs.products.mapIndexed { index, _ ->
                //     if (index % 2 == 0) even else odd
                // }

                // Randomize inputs
                inputs.products.activations = inputs.products.mapIndexed { index, _ ->
                    (Random().nextDouble() - 0.5) * 2
                }

                network.apply {
                    repeat(2) { bufferedUpdate() }
                }

                val source = inputs.products.activations
                val target = outputs.products.activations
                source sse target
            }.sum()
        }

        onPeek {
            val nc = addNetworkComponent("Network", network)
            placeComponent(nc, 0, 0, 500, 500)
        }

        onBuild { visible ->
            network {
                if (visible) {
                    +inputs.asGroup {
                        label = "Inputs"
                        location = point(0, 100)
                        layout = LineLayout()
                    }

                    +hiddenNodes.asGroup {
                        label = "Compressed layer"
                        location = point(20, 0)
                        layout = LineLayout()
                    }

                    +outputs.asGroup {
                        label = "Outputs"
                        location = point(0, -100)
                        layout = LineLayout()

                    }
                } else {
                    +inputs
                    +hiddenNodes
                    +outputs
                }
                +connections
            }
        }

    }

    val evolution = evaluator(evolutionarySimulation) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 500 || fitness < .1 }
    }

    mainScope.launch {

        workspace.clearWorkspace()

        val progressWindow = ProgressWindow(1000, "Error")

        launch(Dispatchers.Default) {

            val generations = evolution.start().onEachGenerationBest { agent, gen ->
                progressWindow.value = gen
                progressWindow.text = "Error: ${agent.fitness.format(2)}"
            }

            val (best, _) = generations.best

            best.visibleBuild().peek()

            progressWindow.close()
        }

    }
}
