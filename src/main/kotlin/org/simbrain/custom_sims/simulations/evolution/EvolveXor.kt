package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import java.awt.Dimension
import kotlin.random.Random

val evolveXor = newSim {

    class XorGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var inputLayerChromosome = chromosome(2) { add(nodeGene { isClamped = true; upperBound = 1.0; lowerBound = -1.0 }) }
        var hiddenLayerChromosome = chromosome(2) { add(nodeGene { upperBound = 1.0; lowerBound = -1.0 }) }
        var outputLayerChromosome = chromosome(1) { add(nodeGene { upperBound = 1.0; lowerBound = -1.0 }) }
        var connectionChromosome = chromosome(1) {
            add(connectionGene(inputLayerChromosome.sampleOne(random), hiddenLayerChromosome.sampleOne(random)))
            add(connectionGene(hiddenLayerChromosome.sampleOne(random), outputLayerChromosome.sampleOne(random)))
        }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network, network.express(inputLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "input" },
                NeuronCollection(network, network.express(hiddenLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "hidden" },
                NeuronCollection(network, network.express(outputLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "output" },
                network.express(connectionChromosome)
            )
        }

        fun copy() = XorGenotype(random.nextLong()).apply {
            val current = this@XorGenotype
            val new = this@apply

            new.inputLayerChromosome = current.inputLayerChromosome.copy()
            new.hiddenLayerChromosome = current.hiddenLayerChromosome.copy()
            new.outputLayerChromosome = current.outputLayerChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
        }

        fun mutate() {
            hiddenLayerChromosome.forEach {
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
            val availableInputToHidden = (inputLayerChromosome cartesianProduct hiddenLayerChromosome) - existingConnections
            val availableHiddenToTarget = (hiddenLayerChromosome cartesianProduct outputLayerChromosome) - existingConnections
            if (random.nextDouble() < 0.25 && availableInputToHidden.isNotEmpty() && availableHiddenToTarget.isNotEmpty()) {
                val (source, target) = if (random.nextBoolean()) {
                    // Make a new connection from the input to hidden layer
                    availableInputToHidden.sampleOne()
                } else {
                    // Make a new connection from the hidden to output layer
                    availableHiddenToTarget.sampleOne()
                }
                connectionChromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.1) {
                hiddenLayerChromosome.add(nodeGene())
            }

        }

    }

    class XorSim(
        val xorGenotype: XorGenotype = XorGenotype(),
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val networkComponent = NetworkComponent("network 1").also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        private val _phenotype = CompletableDeferred<XorGenotype.Phenotype>()
        val phenotype: Deferred<XorGenotype.Phenotype> by this::_phenotype

        override fun mutate() {
            xorGenotype.mutate()
        }

        override suspend fun build() {
            if (!_phenotype.isCompleted) {
                _phenotype.complete(xorGenotype.expressWith(network))
            }
        }

        override fun visualize(workspace: Workspace): XorSim {
            return XorSim(xorGenotype.copy(), workspace)
        }

        override fun copy(): EvoSim {
            return XorSim(xorGenotype.copy(), Workspace())
        }

        override suspend fun eval(): Double {
            build()
            val testData = listOf(
                listOf(0.0, 0.0) to listOf(0.0),
                listOf(0.0, 1.0) to listOf(1.0),
                listOf(1.0, 0.0) to listOf(1.0),
                listOf(1.0, 1.0) to listOf(0.0)
            )

            return testData.sumOf { (input, output) ->
                phenotype.await().inputs.neuronList.activations = input
                // Iterate more each run if allowing recurrent connections
                workspace.iterateSuspend(2)
                val error = (phenotype.await().outputs.neuronList.activations sse output)
                -error
            }
        }

    }

    val maxGenerations = 500
    val progressWindow = ProgressWindow(maxGenerations, "Error").apply {
        minimumSize = Dimension(300, 100)
        setLocationRelativeTo(null)
    }
    val lastGeneration = evaluator(
        populatingFunction = { XorSim() },
        populationSize = 100,
        eliminationRatio = 0.5,
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
            nthPercentileFitness(5) > -0.01 || generation > maxGenerations
        }
    )

    lastGeneration.take(1).forEach {
        with(it.visualize(workspace) as XorSim) {
            build()
            val phenotype = this.phenotype.await()
            phenotype.inputs.neuronList.forEach { it.increment = 1.0 }
            phenotype.inputs.location = point( 0, 150)
            phenotype.hiddens.location = point( 0, 60)
            phenotype.outputs.location = point(0, -25)
        }
    }
    progressWindow.close()
}
