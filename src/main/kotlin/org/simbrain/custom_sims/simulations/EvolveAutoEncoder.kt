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
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import java.util.*

val evolveAutoAssociator = newSim {

    val mainScope = MainScope()

    val evolutionarySimulation = evolutionarySimulation {

        val network = Network()

        val inputs = chromosome(8) { index ->
            nodeGene {
                isClamped = true
                label = "Input ${index + 1}"
            }
        }

        val nodes = chromosome(2) { nodeGene() }

        val outputs = chromosome(8) { index ->
            nodeGene {
                label = "Output ${index + 1}"
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

            nodes.forEach { it.mutateBias() }

            connections.forEach { it.mutateWeight() }

            val (source, target) = if (Random().nextBoolean()) {
                val source = (inputs.genes + nodes.genes).shuffled().first()
                val target = nodes.genes.shuffled().first()
                Pair(source, target)
            } else {
                val source = nodes.genes.shuffled().first()
                val target = (outputs.genes + nodes.genes).shuffled().first()
                Pair(source, target)
            }

            connections.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
        }

        onEval {
            (0..5).map {
                val even = (Random().nextDouble() - 0.5) * 2
                val odd = (Random().nextDouble() - 0.5) * 2
                inputs.products.activations = inputs.products.mapIndexed { index, _ ->
                    if (index % 2 == 0) even else odd
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
            placeComponent(nc, 0, 0, 200, 200)

        }

        onBuild { visible ->
            network {
                if (visible) {
                    +inputs.asGroup {
                        label = "Input"
                        location = point(0, 100)
                    }

                    val (n1, n2) = +nodes
                    n1.location = point(50, 0)
                    n2.location = point(100, 0)

                    +outputs.asGroup {
                        label = "Output"
                        location = point(0, -100)
                    }
                } else {
                    +inputs
                    +nodes
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
        runUntil { generation == 500 || fitness < .2 }
    }

    mainScope.launch {

        workspace.clearWorkspace()

        val progressWindow = ProgressWindow(1000)

        launch(Dispatchers.Default) {

            val generations = evolution.start().onEachGenerationBest { agent, gen ->
                progressWindow.progressBar.value = gen
                progressWindow.fitnessScore.text = "Error: ${agent.fitness.format(2)}"
            }

            val (best, _) = generations.best

            best.visibleBuild().peek()

            progressWindow.close()
        }

    }
}
