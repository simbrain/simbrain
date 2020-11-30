package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
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

//                sim.addNetwork(
//                        best.prettyBuild().evaluationContext.workspace.componentList.first() as NetworkComponent,
//                        0, 200, 200, 0
//                )

                progressWindow.close()
            }

        }

    }

    val evolution: Evaluator get() {

        val environmentBuilder = environmentBuilder {

            val workspace = useWorkspace()
            val network = useNetwork()

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
                nodes.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                    }
                }
                connections.eachMutate {
                    strength += (Random().nextDouble() - 0.5) * 0.2
                }
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
                    workspace.product.apply { repeat(2) { simpleIterate() } }

                    repeat(2) {
                        workspace.product.simpleIterate()
                    }

                    val thing by workspace

                    thing.simpleIterate()

                    workspace {
                        repeat(2) {
                            simpleIterate()
                        }
                    }

                    val source = inputs.products.activations
                    val target = outputs.products.activations
                    source sse target
                }.sum()
            }

            onBuild { pretty ->
                workspace {
                    network {
                        if (pretty) {
                            +inputs.asGroup {
                                label = "Input"
                                location = point(0, 100)
                            }
                            +nodes {
                                this[0].location = point(50, 0)
                                this[1].location = point(100, 0)
                            }
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