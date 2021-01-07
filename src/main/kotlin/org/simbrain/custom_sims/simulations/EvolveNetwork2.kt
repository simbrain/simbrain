package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.network.util.activations
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.place
import org.simbrain.util.point
import java.io.File
import java.util.*
import kotlin.math.abs

/**
 * Evolve a network using a layout object.
 */
val evolveNetwork2 = newSim {

    val environmentBuilder = environmentBuilder {

        val network = Network()

        val nodeChromosome = chromosome(20) {
            nodeGene() {
                location = point(Math.random() * 100, Math.random() * 100)
            }
        }

        val connectionChromosome = chromosome<Synapse, ConnectionGene>()

        val layoutChromosome = chromosome(1) {
            layoutGene()
        }

        onMutate {

            // Local functions that define how to mutate genes
            fun LayoutGene.mutateParam() = mutate {
                hSpacing += random.nextDouble(-1.0, 1.0)
                vSpacing += random.nextDouble(-1.0, 1.0)
            }

            fun LayoutGene.mutateType() = mutate {
                when (random.nextDouble()) {
                    in 0.0..0.05 -> layout = GridLayout()
                    in 0.05..0.1 -> layout = HexagonalGridLayout()
                    in 0.1..0.15 -> layout = LineLayout()
                }
            }

            fun NodeGene.mutateBias() = mutate {
                updateRule.let {
                    if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                }
            }

            fun ConnectionGene.mutateWeight() = mutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }

            // Apply mutations across chromosomes

            // New nodes
            if (Random().nextDouble() > .95) {
                nodeChromosome.genes.add(nodeGene())
            }

            nodeChromosome.genes.forEach {
                it.mutateBias()
            }

            // Modify the layout
            layoutChromosome.genes.forEach {
                it.mutateParam()
                it.mutateType()
            }

            // New connections
            val source = nodeChromosome.genes.shuffled().first()
            val target = nodeChromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })

            // Weight mutations
            connectionChromosome.forEach { it.mutateWeight() }
        }

        onEval {
            repeat(10) { network.bufferedUpdate() }
            abs(nodeChromosome.products.activations.average() - .5)
        }

        onPeek {

            val networkComponent = addNetworkComponent("50 Percent Active", network)
            withGui {
                place(networkComponent) {
                    width = 400
                    height = 400
                }
            }
            // When run headless
            if (desktop == null) {
                workspace.save(File("fiftyPercentActive.zip"))
            }
        }

        onBuild { pretty ->
            val (layout) = +layoutChromosome
            network {
                (+nodeChromosome).apply {
                    layout.layoutNeurons(this)
                }
                +connectionChromosome
            }
        }

    }

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 10 || fitness < .01 }
    }

    workspace.clearWorkspace()

    val generations = evolution.start()
    val (best, _) = generations.last().first()

    best.prettyBuild().peek()

//        val thing = environmentBuilder.build()
//
//        thing.peek()

}

fun main() {
    evolveNetwork2.run()
}
