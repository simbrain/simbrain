package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import java.util.*

val evolveXor = newSim {

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
                    strength += (Random().nextDouble() - 0.5) * 0.2
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
                strength = (Random().nextDouble() - 0.5) * 0.2
            })
            when (Random().nextDouble()) {
                in 0.9..0.95 -> hiddenNodeChromosome.genes.add(nodeGene())
                //in 0.95..1.0 ->  hiddenNodeChromosome.genes.removeAt(0)
            }
        }

        onEval {
            val inputData = listOf(listOf(0.0, 0.0), listOf(1.0, 0.0), listOf(0.0, 1.0), listOf(1.0, 1.0))
            val tarData = listOf(listOf(0.0), listOf(1.0), listOf(1.0), listOf(0.0))
            inputData.zip(tarData).map { (i, t) ->
                inputChromosome.products.activations = i
                network.apply {
                    repeat(20) { bufferedUpdate() }
                }
                t sse outputChromosome.products.activations
            }.sum()
        }

        onPeek {
            addNetworkComponent("Network", network)
        }

        onBuild { pretty ->
            network {
                if (pretty) {
                    +inputChromosome.asGroup {
                        label = "Input"
                        location = point(0, 100)
                    }
                    (+hiddenNodeChromosome).also {
                        LineLayout().layoutNeurons(it)
                    }
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

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 1000 || fitness < .001 }
    }

    MainScope().launch {

        workspace.clearWorkspace()

        val progressWindow = ProgressWindow(1000)

        launch(Dispatchers.Default) {

            val generations = evolution.start().onEachGenerationBest { agent, gen ->
                progressWindow.progressBar.value = gen
                progressWindow.fitnessScore.text = "Error: ${agent.fitness.format(2)}"
            }

            val (best, _) = generations.best

            best.prettyBuild().peek()

            progressWindow.close()
        }

    }

}