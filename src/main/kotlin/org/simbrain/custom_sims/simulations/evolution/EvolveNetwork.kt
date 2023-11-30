package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.bound
import org.simbrain.network.core.*
import org.simbrain.network.layouts.Layout
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.cartesianProduct
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.point
import org.simbrain.util.sampleOne
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import java.awt.Dimension
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.abs
import kotlin.random.Random

/**
 * Evolve a network. Several fitness functions are included which can be commented on or off.
 */
val evolveNetwork = newSim {

    class EvolveNetworkGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var layoutChromosome = chromosome(1) { add(layoutGene()) }
        var motivationChromosome = chromosome(2) { add(nodeGene {
                label = "Fixed node ${it+1}"
                location = point(it * 100,-50)
                lowerBound = -10.0
                upperBound = 10.0
            })
        }
        var nodeChromosome = chromosome(2) {
            add(nodeGene { upperBound = 10.0; lowerBound = -10.0 })
        }
        var connectionChromosome = chromosome<Synapse, ConnectionGene>()

        inner class Phenotype(
            val layout: Layout,
            val motivations: List<Neuron>,
            val nodes: List<Neuron>,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            val layout = express(layoutChromosome).first().express()
            val motivations = network.express(motivationChromosome)
            val nodes = network.express(nodeChromosome)
            val connections = network.express(connectionChromosome)
            layout.layoutNeurons(nodes)
            return Phenotype(layout, motivations, nodes, connections)
        }

        fun copy() = EvolveNetworkGenotype(random.nextLong()).apply {
            val current = this@EvolveNetworkGenotype
            val new = this@apply

            new.layoutChromosome = current.layoutChromosome.copy()
            new.motivationChromosome = current.motivationChromosome.copy()
            new.nodeChromosome = current.nodeChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
        }

        fun mutate() {
            layoutChromosome.forEach {
                it.mutateParam()
                it.mutateType()
            }

            nodeChromosome.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            motivationChromosome.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            connectionChromosome.forEach {
                it.mutate {
                    strength += random.nextDouble(-1.0, 1.0)
                }
            }

            // Ensure existing connections are not used when creating new connections
            val existingConnections = connectionChromosome.map { it.source to it.target }.toSet()
            val availableConnections = (nodeChromosome cartesianProduct nodeChromosome) - existingConnections
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne()
                connectionChromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.1) {
                nodeChromosome.add(nodeGene())
            }

        }

    }

    class EvolveNetworkSim(
        val evolveNetworkGenotype: EvolveNetworkGenotype = EvolveNetworkGenotype(),
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val networkComponent = NetworkComponent("network 1").also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        private val _phenotype = CompletableDeferred<EvolveNetworkGenotype.Phenotype>()
        val phenotype: Deferred<EvolveNetworkGenotype.Phenotype> by this::_phenotype

        override fun mutate() {
            evolveNetworkGenotype.mutate()
        }

        override suspend fun build() {
            if (!_phenotype.isCompleted) {
                _phenotype.complete(evolveNetworkGenotype.expressWith(network))
            }
        }

        override fun visualize(workspace: Workspace): EvolveNetworkSim {
            return EvolveNetworkSim(evolveNetworkGenotype.copy(), workspace)
        }

        override fun copy(): EvoSim {
            return EvolveNetworkSim(evolveNetworkGenotype.copy(), Workspace())
        }

        override suspend fun eval(): Double {
            build()
            val phenotype = phenotype.await()
            // Iterate to stabilize network but sometimes fails for small numbers.
            repeat(50) { network.bufferedUpdate() }
            // Comment / Uncomment different choices of fitness function here
            suspend fun fitness(): Double {
                val avgLength = phenotype.connections.lengths.average()
                val numWeights = phenotype.connections.size
                val avgActivation = phenotype.nodes.activations.average()
                val totalActivation = phenotype.nodes.activations.sum()
                // Evolve fixed nodes to have specific activations 2.5 and -3
                val (m1, m2) =  phenotype.motivations
                val m1error = abs(m1.activation - 2.5)
                val m2error = abs(m2.activation + 3)
                // TODO: Normalize errors and provide for weightings
                val numNodesError = abs(phenotype.nodes.size - 20).toDouble()
                val numWeightsError = abs(numWeights - 40)
                val axonLengthError = abs(avgLength - 250)
                val avgActivationError = abs(avgActivation - 5)
                val totalActivationError = abs(totalActivation - 10)
                // Area in thousands of pixels
                val bounds  = network.freeNeurons.bound
                val size = (bounds.height * bounds.width) / 10_000
                val areaError = abs(size - 10)
                return numNodesError + totalActivationError
            }
            // print("${network.looseNeurons.size},")
            return -fitness()
        }

    }

    val maxGenerations = 500
    val progressWindow = ProgressWindow(maxGenerations, "Error").apply {
        minimumSize = Dimension(300, 100)
        setLocationRelativeTo(null)
    }
    val lastGeneration = evaluator(
        populatingFunction = { EvolveNetworkSim() },
        populationSize = 1000,
        eliminationRatio = 0.25,
        peek = {
            listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                "$it: ${nthPercentileFitness(it).format(3)}"
            }.also {
                println("[$generation] $it")
                progressWindow.text = "5th Percentile MSE: ${nthPercentileFitness(5).format(3)}"
                progressWindow.value = generation
            }
        },
        stoppingFunction = {
            nthPercentileFitness(5) > -0.1 || generation > maxGenerations
        }
    )

    lastGeneration.take(1).forEach {
        with(it.visualize(workspace) as EvolveNetworkSim) {
            build()
            val phenotype = this.phenotype.await()
        }
    }
    progressWindow.close()
}
