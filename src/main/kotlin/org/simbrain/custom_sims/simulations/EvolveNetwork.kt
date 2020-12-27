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
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.gui.SimbrainDesktop
import java.util.*
import kotlin.math.abs

/**
 * Evolve a network whose average activation is a specified value.
 */
class EvolveNetwork(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Network"

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

            val network = useNetwork()

            val nodeChromosome = chromosome(20) {
                nodeGene() {
                    location = point(Math.random()*100, Math.random()*100)
                }
            }

            val connectionChromosome = chromosome<Synapse, ConnectionGene>()

            onMutate {

                // New nodes
                if (Random().nextDouble() > .95) {
                    nodeChromosome.genes.add(nodeGene())
                }

                // Change bias of nodes
                nodeChromosome.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                    }
                }

                // New connections
                val source = nodeChromosome.genes.shuffled().first()
                val target =  nodeChromosome.genes.shuffled().first()
                connectionChromosome.genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5 ) * 0.2
                })

                // Weight mutations
                connectionChromosome.eachMutate {
                    strength += (Random().nextDouble() - 0.5 ) * 0.2
                }
            }

            onEval {
                repeat(10) { network.product.bufferedUpdate() }
                abs(nodeChromosome.products.activations.average() - .5)
            }

            onPeek {
                sim.addNetwork(network { NetworkComponent("Network", this) }, 0, 200, 200, 0)
            }

            onBuild { pretty ->
                network {
                    +nodeChromosome
                    +connectionChromosome
                }
            }

        }

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
            runUntil { generation == 200 || fitness < .01 }
        }

    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveNetwork(desktop)
    }

}