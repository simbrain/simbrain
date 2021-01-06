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

class EvolveXor(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve XOR"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(1000)

            launch(Dispatchers.Default) {

                val generations = evolution.start().onEachIndexed { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Error: ${result[0].fitness.format(10)}"
                }
                val (best, _) = generations.last().first()

                best.prettyBuild().peek()

                progressWindow.close()
            }

        }

    }

    val evolution: Evaluator get() {

        val environmentBuilder = environmentBuilder {

            val network = Network()

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

            val connectionChromosome = chromosome<Synapse, ConnectionGene>()

            onMutate {
                hiddenNodeChromosome.forEach {
                    it.mutate {
                        updateRule.let {
                            if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                        }
                    }
                }
                connectionChromosome.forEach {
                    it.mutate {
                        strength += (Random().nextDouble() - 0.5 ) * 0.2
                    }
                }
                // Either connect input to hidden or hidden to output, or hidden to hidden
                val (source, target) = if (Random().nextBoolean()) {
                    val source = (inputChromosome.genes + hiddenNodeChromosome.genes).shuffled().first()
                    val target = hiddenNodeChromosome.genes.shuffled().first()
                    Pair(source, target)
                } else {
                    val source = hiddenNodeChromosome.genes.shuffled().first()
                    val target = (outputChromosome.genes + hiddenNodeChromosome.genes).shuffled().first()
                    Pair(source, target)
                }
                connectionChromosome.genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5 ) * 0.2
                })
                when (Random().nextDouble()) {
                    in 0.9..0.95 ->  hiddenNodeChromosome.genes.add(nodeGene())
                    //in 0.95..1.0 ->  hiddenNodeChromosome.genes.removeAt(0)
                }
            }

            onEval {
                val inputData = listOf(listOf(0.0,0.0), listOf(1.0,0.0), listOf(0.0,1.0), listOf(1.0,1.0))
                val tarData = listOf(listOf(0.0), listOf(1.0), listOf(1.0), listOf(0.0))
                inputData.zip(tarData).map {(i, t) ->
                    inputChromosome.products.activations = i
                    network.apply {
                        repeat(20) { bufferedUpdate() }
                    }
                    t sse outputChromosome.products.activations
                }.sum()
            }

            onPeek {
                sim.addNetwork(NetworkComponent("Network", network), 0, 200, 200, 0)
            }

            onBuild { pretty ->
                network {
                    if (pretty) {
                        +inputChromosome.asGroup {
                            label = "Input"
                            location = point(0, 100)
                        }
                        +hiddenNodeChromosome
                        +outputChromosome.asGroup {
                            label = "Output"
                            location = point(0, -100)
                        }
                    } else {
                        +inputChromosome
                        +hiddenNodeChromosome
                        +outputChromosome
                    }
                    +connectionChromosome
                }
            }

        }

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
            runUntil { generation == 200 || fitness < .1 }
        }

    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveXor(desktop)
    }

}