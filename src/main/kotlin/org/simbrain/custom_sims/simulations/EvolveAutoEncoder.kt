package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.gui.SimbrainDesktop
import java.util.*

class EvolveAutoEncoder(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Auto Encoder"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(1000)

            launch(Dispatchers.Default) {

                val generations = evolution.start().onEachIndexed { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Error: ${result[0].fitness.format(2)}"
                }
                val (best, _) = generations.last().first()

                best.copy().prettyBuild().peek()

                progressWindow.close()
            }

        }

    }

    val evolution: Evaluator get() {

        val environmentBuilder = environmentBuilder {

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
                sim.addNetwork(
                        NetworkComponent("Network", network),
                        0, 200, 200, 0
                )
            }

            onBuild { pretty ->
                network {
                    if (pretty) {
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

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
            runUntil { generation == 500 || fitness < .2 }
        }

    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveAutoEncoder(desktop)
    }

}