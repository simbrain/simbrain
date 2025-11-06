package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
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
        targetMetric = 0.01,
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
                NeuronCollection(network.express(inputLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "input" },
                NeuronCollection(network.express(hiddenLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "hidden" },
                NeuronCollection(network.express(outputLayerChromosome)).also { network.addNetworkModelAsync(it); it.label = "output" },
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
                    bias += random.nextDouble(-1.0, 1.0)
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

        addSidebarInfo(
        """ 
        # Evolving A Network for XOR
        
        This is a simulation of the evolution of a neural network evolving to solve the XOR problem using an evolutionary framework in Simbrain. 
        
        # Simulation Details
        
        This simulation simulates the evolution of a neural network until the `target error` in the control panel is met, exceeded, or when it has reached the `maximum generation`. The
        goal of this simulation is to evolve until it is as close as possible to the `target error`. 
        
        In this simulation, the `target error` is calculated as the difference between the existing dataset (i.e., neuron groups) and the actual solution to the XOR problem.
        
        For a comprehensive look into how evolutionary simulations are developed in Simbrain, look [here](https://docs.simbrain.net/docs/evolution/).
        
        ## Evolutionary Process
        
        The evolutionary process begins with a starting `population size` of simulations. In generation `0`, each simulation is created with a three-layer network of the XOR solution and a preset amount of 
        connections (`1` per layer). Within each generation, the simulation will iterate until the specified value while the fitness of each simulation is calculated and recorded.
        
        Then after each generation, a percentage of the population is eliminated (e.g., `elimination ratio`) and repopulated with new simulations. During this process of reproduction, some of the new simulations
        will have mutations, where the simulation develops new neurons in the hidden layer (`10%` chance), connections between neuron layers (`25%` chance), changes in neuron biases and weight strengths. 
        
        After each generation, a percentage of the top performer(s) is evaluated (e.g, `Evaluation percentile`) to determine if the `target error` has been achieved. This process continues until the simulation has 
        reached the `target error` or lower, or when the evolutionary process ends. 
        
        # What to Do    
            
        In this simulation, similar to the other evolutionary simulations, the control panel controls how the evolutionary process works. Below are the steps to evolving the simulation:
        
        1) Specify the parameters of the simulation.
        
        2) After confirming the parameters are what you want, click on the `Evolve` button to start the simulation.
        
        3) Now, wait for the evolution process to finish, note that it can take a while depending on your configurations.

        # Credits
 
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
        
        """.trimIndent()
        )

    }
}
