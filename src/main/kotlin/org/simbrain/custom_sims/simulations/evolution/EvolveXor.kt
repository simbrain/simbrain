package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.workspace.Workspace
import kotlin.random.Random

val evolveXor = newSim {

    val evaluatorParams = EvaluatorParams(
        populationSize = 100,
        eliminationRatio = 0.5,
        targetValue = 0.01,
        stoppingCondition = EvaluatorParams.StoppingCondition.Error,
        maxGenerations = 500,
        iterationsPerRun = 2,
        seed = 42
    )

    class XorGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var inputLayerChromosome = chromosome(2) { add(nodeGene { clamped = true; upperBound = 1.0; lowerBound = -1.0 }) }
        var hiddenLayerChromosome = chromosome(2) { add(nodeGene { upperBound = 1.0; lowerBound = -1.0 }) }
        var outputLayerChromosome = chromosome(1) { add(nodeGene { upperBound = 1.0; lowerBound = -1.0 }) }
        var connectionChromosome = chromosome(1) {
            createGene(inputLayerChromosome to hiddenLayerChromosome) {
                strength = random.nextDouble(-1.0, 1.0)
            }
            createGene(hiddenLayerChromosome to outputLayerChromosome) {
                strength = random.nextDouble(-1.0, 1.0)
            }
        }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network.express(inputLayerChromosome)).also { network.addNetworkModel(it); it.label = "input" },
                NeuronCollection(network.express(hiddenLayerChromosome)).also { network.addNetworkModel(it); it.label = "hidden" },
                NeuronCollection(network.express(outputLayerChromosome)).also { network.addNetworkModel(it); it.label = "output" },
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

            withProbability(0.25) {
                connectionChromosome.createGene(
                    inputLayerChromosome to hiddenLayerChromosome,
                    hiddenLayerChromosome to outputLayerChromosome
                ) { strength = random.nextDouble(-1.0, 1.0) }
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
                workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
                val error = (phenotype.await().outputs.neuronList.activations sse output)
                error
            }
        }

    }

    suspend fun runSim() {
        val lastGeneration = evaluator(
            evaluatorParams,
            populatingFunction = { XorSim(XorGenotype(seed = seed)) }
        )
        lastGeneration.take(1).forEach {
            with(it.visualize(workspace) as XorSim) {
                build()
                val phenotype = this.phenotype.await()
                phenotype.inputs.neuronList.forEach { it.increment = 1.0 }
                phenotype.inputs.location = point( 0, 150)
                phenotype.hiddens.location = point( 0, 60)
                phenotype.outputs.location = point(0, -25)
                withGui {
                    place(networkComponent, 340, 10, 384, 480)
                }
            }
        }
    }

    withGui {
        workspace.clearWorkspace()
        evaluatorParams.createControlPanel("Control Panel", 5, 10)
        evaluatorParams.addControlPanelButton("Evolve") {
            workspace.removeAllComponents()
            evaluatorParams.addProgressWindow()
            runSim()
        }
    }
}
