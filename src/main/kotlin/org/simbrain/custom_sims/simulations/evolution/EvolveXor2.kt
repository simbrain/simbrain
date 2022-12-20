package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithm2.*
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import kotlin.random.Random

val evolveXor2 = newSim {

    val coroutineScope = workspace.coroutineScope

    class XorGenotype(seed: Long = Random.nextLong()) : Genotype2 {

        override val random: Random = Random(seed)

        var inputs = chromosome2(2) { add(nodeGene2 { isClamped = true }) }
        var hiddens = chromosome2(2) { add(nodeGene2()) }
        var outputs = chromosome2(1) { add(nodeGene2()) }
        var connections = chromosome2(0) { add(connectionGene2(inputs.first(), outputs.first())) }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun build(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network, network.express(inputs)).also { network.addNetworkModel(it); it.label = "input" },
                NeuronCollection(network, network.express(hiddens)).also { network.addNetworkModel(it); it.label = "hidden" },
                NeuronCollection(network, network.express(outputs)).also { network.addNetworkModel(it); it.label = "output" },
                network.express(connections)
            )
        }

        fun copy() = XorGenotype(random.nextLong()).apply {
            val current = this@XorGenotype
            val new = this@apply

            new.inputs = current.inputs.copy()
            new.hiddens = current.hiddens.copy()
            new.outputs = current.outputs.copy()
            new.connections = current.connections.copy()
        }

        fun mutate() {
            hiddens.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            connections.forEach {
                it.mutate {
                    strength += random.nextDouble(-1.0, 1.0)
                }
            }

            val existingPairs = connections.map { it.source to it.target }.toSet()

            val availableInputToHidden = (inputs cartesianProduct hiddens) - existingPairs
            val availableHiddenToTarget = (hiddens cartesianProduct outputs) - existingPairs

            if (random.nextDouble() < 0.25 && availableInputToHidden.isNotEmpty() && availableHiddenToTarget.isNotEmpty()) {
                val (source, target) = if (random.nextBoolean()) {
                    availableInputToHidden.sampleOne()
                } else {
                    availableHiddenToTarget.sampleOne()
                }
                connections.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.1) {
                hiddens.add(nodeGene2())
            }

        }

    }

    class Xor2Sim(
        val xor2Genotype: XorGenotype = XorGenotype(),
        val workspace: Workspace = HeadlessWorkspace()
    ) : EvoSim {

        val networkComponent = NetworkComponent("network 1").also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        private val _phenotype = CompletableDeferred<XorGenotype.Phenotype>()
        val phenotype: Deferred<XorGenotype.Phenotype> by this::_phenotype

        override fun mutate() {
            xor2Genotype.mutate()
        }

        override suspend fun build() {
            if (_phenotype.isActive) {
                _phenotype.complete(xor2Genotype.build(network))
            }
        }

        override fun visualize(workspace: Workspace): Xor2Sim {
            return Xor2Sim(xor2Genotype.copy(), workspace)
        }

        override fun copy(): EvoSim {
            return Xor2Sim(xor2Genotype.copy(), HeadlessWorkspace())
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
                // Iterate more each run if allowing recurent connections
                workspace.iterateSuspend(2)
                val error = (phenotype.await().outputs.neuronList.activations sse output)
                -error
            }
        }

    }

    workspace.coroutineScope.launch {
        val maxGeneratioms = 500
        val progressWindow = ProgressWindow(maxGeneratioms, "Error")
        val lastGeneration = evaluator2(
            populatingFunction = { Xor2Sim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            peek = {
                progressWindow.value = generation
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${nthPercentileFitness(it).format(3)}"
                }.also {
                    println("[$generation] $it")
                    progressWindow.text = "Error: ${nthPercentileFitness(0).format(3)}"
                }
            },
            stoppingFunction = {
                nthPercentileFitness(5) > -0.05 || generation > maxGeneratioms
            }
        )

        lastGeneration.take(1).forEach {
            with(it.visualize(workspace) as Xor2Sim) {
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
}
