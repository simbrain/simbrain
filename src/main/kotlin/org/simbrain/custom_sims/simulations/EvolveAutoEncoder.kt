package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.neat.gui.ProgressWindow
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.workspace.gui.SimbrainDesktop
import java.util.*
import kotlin.streams.toList

class EvolveAutoEncoder(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Auto Encoder"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(1000)

            launch(Dispatchers.Default) {
                val generations = evolve { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Error: ${result[0].second.format(2)}"
                }
                val (best, _) = generations.last().first()

                sim.addNetwork(
                        best.prettyBuild().evaluationContext.workspace.componentList.first() as NetworkComponent,
                        0, 200, 200, 0
                )

                progressWindow.close()
            }

        }

    }

    fun evolve(peek: (generation: Int, result: List<Pair<EnvironmentBuilder, Double>>) -> Unit): Sequence<List<Pair<EnvironmentBuilder, Double>>> {
        val environmentBuilder = environmentBuilder {

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

            val connections = chromosome<Synapse, ConnectionGene5>()

            onMutate {
                nodes.current.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                    }
                }
                connections.current.eachMutate {
                    strength += (Random().nextDouble() - 0.5 ) * 0.2
                }
                val (source, target) = if (Random().nextBoolean()) {
                    val source = (inputs.current.genes + nodes.current.genes).shuffled().first()
                    val target = nodes.current.genes.shuffled().first()
                    Pair(source, target)
                } else {
                    val source = nodes.current.genes.shuffled().first()
                    val target = (outputs.current.genes + nodes.current.genes).shuffled().first()
                    Pair(source, target)
                }
                connections.current.genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5 ) * 0.2
                })
            }

            onEval {
                (0..5).map {
                    val even = (Random().nextDouble() - 0.5) * 2
                    val odd = (Random().nextDouble() - 0.5) * 2
                    inputs.products.activations = inputs.products.mapIndexed { index, _ ->
                        if (index % 2 == 0) even else odd
                    }
                    repeat(2) {
                        workspace.simpleIterate()
                    }
                    val source = inputs.products.activations
                    val target = outputs.products.activations
                    source sse target
                }.sum()
            }

            onBuild {
                +network {
                    +inputs
                    +nodes
                    +outputs
                    +connections
                }
            }

            onPrettyBuild {
                +network {
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
                    +connections
                }
            }

        }

        val population = generateSequence(environmentBuilder.copy()) { it.copy() }.take(100).toList()

        return sequence {
            var next = population
            while (true) {
                val current = next.parallelStream().map {
                    val build = it.build()
                    val score = build.eval()
                    Pair(it, score)
                }.toList().sortedBy { it.second }
                val survivors = current.take(current.size / 2)
                next = survivors.map { it.first } + survivors.parallelStream().map { it.first.copy().apply { mutate() } }.toList()
                yield(current)
            }
        }.onEachIndexed(peek).take(1000).takeWhile { it[0].second > 0.2 }
    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveAutoEncoder(desktop)
    }

}