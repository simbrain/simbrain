package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Synapse
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

        val network = useNetwork()

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

            // Modify the layout
            layoutChromosome.eachMutate {
                hSpacing += Random().nextDouble()
                vSpacing += Random().nextDouble()
            }

            // New connections
            val source = nodeChromosome.genes.shuffled().first()
            val target = nodeChromosome.genes.shuffled().first()
            connectionChromosome.genes.add(connectionGene(source, target) {
                strength = (Random().nextDouble() - 0.5) * 0.2
            })

            // Weight mutations
            connectionChromosome.eachMutate {
                strength += (Random().nextDouble() - 0.5) * 0.2
            }
        }

        onEval {
            repeat(10) { network.product.bufferedUpdate() }
            abs(nodeChromosome.products.activations.average() - .5)
        }

        onPeek {
            val nc = addNetworkComponent("Network", network.product)
            withGui {
                place(nc) {
                    width = 200
                    height = 200
                }
            }
            // When run headless
            if (desktop == null) {
                workspace.save(File("fiftyPercentActive.zip"))
            }
        }

        onBuild { pretty ->
            network {
                +nodeChromosome
                +connectionChromosome
            }
        }

    }

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 200 || fitness < .01 }
    }

    workspace.clearWorkspace()

    val generations = evolution.start()
    val (best, _) = generations.last().first()

    best.prettyBuild().peek()

}

