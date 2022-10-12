package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm2.*
import org.simbrain.util.point
import org.simbrain.util.sampleWithoutReplacement
import org.simbrain.util.sse
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import kotlin.random.Random

val evolveXor2 = newSim {

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

            if (random.nextDouble() < 0.9) {
                val (source, target) = if (random.nextBoolean()) {
                    val source = hiddens.toList().sampleWithoutReplacement().first()
                    val target = outputs.toList().sampleWithoutReplacement().first()
                    source to target
                } else {
                    val source = inputs.sampleWithoutReplacement().first()
                    val target = hiddens.sampleWithoutReplacement().first()
                    source to target
                }
                connections.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            if (random.nextDouble() < 0.1) {
                hiddens.add(nodeGene2())
            }

        }

    }

    class Xor2Sim(
        val xor2Genotype: XorGenotype = XorGenotype(),
        val workspace: Workspace = Workspace(GlobalScope)
    ) : EvoSim {

        val networkComponent = NetworkComponent("network 1").also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        val phenotype = runBlocking { xor2Genotype.build(network) }

        override fun mutate() {
            xor2Genotype.mutate()
        }

        override fun copy(workspace: Workspace): Xor2Sim {
            return Xor2Sim(xor2Genotype.copy(), workspace)
        }

        override suspend fun eval(): Double {

            val testData = listOf(
                listOf(0.0, 0.0) to listOf(0.0),
                listOf(0.0, 1.0) to listOf(1.0),
                listOf(1.0, 0.0) to listOf(1.0),
                listOf(1.0, 1.0) to listOf(0.0)
            )

            return testData.sumOf { (input, output) ->
                phenotype.inputs.neuronList.activations = input
                workspace.iterateSuspend(20)
                -(phenotype.outputs.neuronList.activations sse output)
            }
        }

    }

    workspace.coroutineScope.launch {
        val maxGeneratioms = 200
        val progressWindow = ProgressWindow(maxGeneratioms, "Error")
        val lastGeneration = evaluator2(
            populatingFunction = { Xor2Sim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            stoppingFunction = {
                progressWindow.value = generation
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${nthPercentileFitness(it).format(3)}"
                }.also {
                    println("[$generation] $it")
                    progressWindow.text = "Error: ${nthPercentileFitness(0).format(3)}"
                }
                nthPercentileFitness(5) > -0.05 || generation > maxGeneratioms
            }
        )

        lastGeneration.take(5).forEach {
            with(it.copy(workspace) as Xor2Sim) {
                phenotype.inputs.neuronList.forEach { it.increment = 1.0 }
                phenotype.inputs.location = point( 0, 150)
                phenotype.hiddens.location = point( 0, 60)
                phenotype.outputs.location = point(0, -25)
            }
        }

        progressWindow.close()
    }
}
