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

class xor5(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve XOR"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(1000)

            launch(Dispatchers.Default) {
                val generations = evolve { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Error: ${result[0].second.format(10)}"
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

    fun evolve(peek: (generation: Int, result: List<Pair<EnvironmentBuilder, Double>>) -> Unit):
            Sequence<List<Pair<EnvironmentBuilder, Double>>> {

        val environmentBuilder = environmentBuilder {

            val inputChromosome = chromosome(2) { index ->
                nodeGene {
                    isClamped = true
                    label = "Input ${index + 1}"
                    increment = 1.0
                }
            }

            val hiddenNodeChromosome = chromosome(2) { nodeGene() }

            val outputChromosome = chromosome(1) { index ->
                nodeGene {
                    label = "Output ${index + 1}"
                }
            }

            val connectionChromosome = chromosome<Synapse, ConnectionGene5>()

            onMutate {
                hiddenNodeChromosome.current.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                    }
                }
                connectionChromosome.current.eachMutate {
                    strength += (Random().nextDouble() - 0.5 ) * 0.2
                }
                // Either connect input to hidden or hidden to output, or hidden to hidden
                val (source, target) = if (Random().nextBoolean()) {
                    val source = (inputChromosome.current.genes + hiddenNodeChromosome.current.genes).shuffled().first()
                    val target = hiddenNodeChromosome.current.genes.shuffled().first()
                    Pair(source, target)
                } else {
                    val source = hiddenNodeChromosome.current.genes.shuffled().first()
                    val target = (outputChromosome.current.genes + hiddenNodeChromosome.current.genes).shuffled().first()
                    Pair(source, target)
                }
                connectionChromosome.current.genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5 ) * 0.2
                })
                when (Random().nextDouble()) {
                    in 0.9..0.95 ->  hiddenNodeChromosome.current.genes.add(nodeGene())
                    //in 0.95..1.0 ->  hiddenNodeChromosome.current.genes.removeAt(0)
                }
            }

            onEval {
                val inputData = listOf(listOf(0.0,0.0), listOf(1.0,0.0), listOf(0.0,1.0), listOf(1.0,1.0))
                val tarData = listOf(listOf(0.0), listOf(1.0), listOf(1.0), listOf(0.0))
                inputData.zip(tarData).map {(i, t) ->
                    inputChromosome.products.activations = i
                    repeat(20) {
                        workspace.simpleIterate()
                    }
                    t sse outputChromosome.products.activations
                }.sum()
            }

            onBuild {
                +network {
                    +inputChromosome
                    +hiddenNodeChromosome
                    +outputChromosome
                    +connectionChromosome
                }
            }

            onPrettyBuild {
                +network {
                    +inputChromosome.asGroup {
                        label = "Input"
                        location = point(0, 100)
                    }
                    +hiddenNodeChromosome
                    +outputChromosome.asGroup {
                        label = "Output"
                        location = point(0, -100)
                    }
                    +connectionChromosome
                }
            }

        }

        // TODO: Move to genetics
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
        }.onEachIndexed(peek).take(1000).takeWhile { it[0].second > 0.01 }
    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return xor5(desktop)
    }

}