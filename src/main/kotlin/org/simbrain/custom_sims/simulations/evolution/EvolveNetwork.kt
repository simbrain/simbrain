package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.layouts.Layout
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.place
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import kotlin.math.abs
import kotlin.random.Random

/**
 * Evolve a network. Several fitness functions are included which can be commented on or off.
 */
val evolveNetwork = newSim {

    // TOOD: Options to use layout mutations, connection mutations, and maybe rule

    val evaluatorParams = EvaluatorParams(
        populationSize = 1000,
        eliminationRatio = 0.25,
        targetMetric = 0.01,
        stoppingCondition = EvaluatorParams.StoppingCondition.Error,
        maxGenerations = 100,
        iterationsPerRun = 50,
        seed = 42
    )

    class NetworkParameters: EditableObject {

        var useNumNodes = true
        var targetNumNodes by GuiEditable(
            initValue = 20,
            description = "How many nodes the network should have",
            useCheckboxFrom = NetworkParameters::useNumNodes,
            order = 10
        )

        var useNumWeights = true
        var targetNumWeights by GuiEditable(
            initValue = 40,
            description = "How many weights the network should have",
            useCheckboxFrom = NetworkParameters::useNumWeights,
            order = 20
        )

        var useTotalActivation = false
        var targetTotalActivation by GuiEditable(
            initValue = 10,
            description = "What the sum of activations over all nodes should be",
            useCheckboxFrom = NetworkParameters::useTotalActivation,
            order = 30
        )

        var useAverageActivation = false
        var targetAverageActivation by GuiEditable(
            initValue = 2.0,
            description = "What the average node activation should be",
            useCheckboxFrom = NetworkParameters::useAverageActivation,
            order = 40
        )

        var useAverageConnectionLength = false
        var targetAverageConnectionLength by GuiEditable(
            initValue = 250.0,
            description = "What the average connection length (pixels between source and target neurons) should be",
            useCheckboxFrom = NetworkParameters::useAverageConnectionLength,
            order = 50
        )

        // TODO: This is probably not the best measure of node dispersion. Replace it with something better.
        var useNodeArea = false
        var targetNodeArea by GuiEditable(
            initValue = 100.0,
            description = "Area spanned by nodes in units of 100s of pixels squared. Larger numbers mean more spread out.",
            useCheckboxFrom = NetworkParameters::useNodeArea,
            order = 60
        )
    }
    val networkParams = NetworkParameters()

    class EvolveNetworkGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var layoutChromosome = chromosome(1) { add(layoutGene()) }
        var nodeChromosome = chromosome(2) {
            add(nodeGene { upperBound = 10.0; lowerBound = -10.0 })
        }
        var connectionChromosome = chromosome<Synapse, ConnectionGene>()

        inner class Phenotype(
            val layout: Layout,
            val nodes: List<Neuron>,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            val layout = express(layoutChromosome).first().express()
            val nodes = network.express(nodeChromosome)
            val connections = network.express(connectionChromosome)
            layout.layoutNeurons(nodes)
            return Phenotype(layout, nodes, connections)
        }

        fun copy() = EvolveNetworkGenotype(random.nextLong()).apply {
            val current = this@EvolveNetworkGenotype
            val new = this@apply

            new.layoutChromosome = current.layoutChromosome.copy()
            new.nodeChromosome = current.nodeChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
        }

        fun mutate() {

            // Mutate layout
            layoutChromosome.forEach {
                it.mutateParam()
                it.mutateType()
            }

            // Add nodes
            if (random.nextDouble() < 0.1) {
                nodeChromosome.add(nodeGene())
            }

            // Mutate biases
            nodeChromosome.forEach {
                it.mutate {
                    bias += random.nextDouble(-1.0, 1.0)
                }
            }

            // Add new connections
            withProbability(0.25) {
                connectionChromosome.createGene(
                    nodeChromosome to nodeChromosome
                ) { strength = random.nextDouble(-1.0, 1.0) }
            }

            // Mutate strengths
            connectionChromosome.forEach {
                it.mutate {
                    strength += random.nextDouble(-1.0, 1.0)
                }
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

            // Iterate network to stabilize network
            repeat(evaluatorParams.iterationsPerRun) { network.bufferedUpdate() }

            // TODO: Normalize errors, provide for weightings, and consider squared error

            var totalError = 0.0

            // Number of nodes
            if (networkParams.useNumNodes) {
                totalError += abs(phenotype.nodes.size - networkParams.targetNumNodes)
            }

            // Num Weights
            if (networkParams.useNumWeights) {
                totalError += abs(phenotype.connections.size - networkParams.targetNumWeights)
            }

            // Average activation
            if (networkParams.useAverageActivation) {
                totalError += abs(phenotype.nodes.activations.average() - networkParams.targetAverageActivation)
            }

            // Total Activation
            if (networkParams.useTotalActivation) {
                totalError += abs(phenotype.nodes.activations.sum() - networkParams.targetTotalActivation)
            }

            // Average length of connections
            if (networkParams.useAverageConnectionLength) {
                totalError += abs(phenotype.connections.lengths.average() - networkParams.targetAverageConnectionLength)
            }

            // "Area" spanned by nodes
            if (networkParams.useNodeArea) {
                val bounds = network.freeNeurons.bound
                val scaledArea = (bounds.height * bounds.width) / 10_000
                totalError += abs(scaledArea - networkParams.targetNodeArea)
            }

            return totalError
        }

    }

    suspend fun runSim() {
        val lastGeneration = evaluator(
            evaluatorParams,
            populatingFunction = { EvolveNetworkSim(EvolveNetworkGenotype(seed = seed)) }
        )
        lastGeneration.take(1).forEach {
            with(it.visualize(workspace) as EvolveNetworkSim) {
                build()
                withGui {
                    place(networkComponent, 340, 10, 384, 480)
                }
            }
        }
    }

    withGui {
        workspace.clearWorkspace()
        val controlPanel = evaluatorParams.createControlPanel("Control Panel", 5, 10)

        controlPanel.addSeparator()
        val propertyEditor = AnnotatedPropertyEditor(networkParams)
        controlPanel.addAnnotatedPropertyEditor(propertyEditor)

        controlPanel.addSeparator()
        evaluatorParams.addControlPanelButton("Evolve") {
            workspace.removeAllComponents()
            evaluatorParams.addProgressWindow()
            propertyEditor.commitChanges()
            println(networkParams.allPropertiesToString())
            runSim()
        }

        addSidebarInfo(
        """ 
        # Introduction
        
        This is a simulation of the evolution of a network towards a target quality (or configuration) using an evolutionary framework in Simbrain. 
        
        # Simulation Details
        
        This simulation simulates the evolution of a neural network until the `target error` in the control panel is met, exceeded, or when it has reached the maximum generation. 
        The goal of this simulation is to evolve until it is as close as possible to the `target error`. The `target error` is calculated as the total sum error of the target 
        qualities (e.g., `Target Num of Nodes` `Target Num of Weights`, etc). Each target quality error is calculated as the difference between current active quality vs 
        active target quality.
        
        The target qualities are explained in the tooltip. To see their explanations, hover over them in the control panel. For a comprehensive look into how evolutionary simulations
        are developed in Simbrain and comprehensive explanation of the target qualities, look [here](https://docs.simbrain.net/docs/evolution/).
        
        ## Evolutionary Process
        
        The simulation begins with a starting population of simulations. In generation 0, each simulation starts with 2 neurons. After each generation, a certain percentage of the 
        population is eliminated (e.g., `elimination ratio`) and repopulated with new simulations. During this process of reproduction, the new simulations will have mutations, where
        the simulation develops more nodes, changes in node biases, weight strengths, the physical layout of the nodes, and new connections between nodes (25%). After each generation, 
        the fitness or, the performance of the simulation is calculated and a percentage of the top performers is evaluated (e.g, `Evaluation percentile`) to determine if the 
        `target error` has been achieved. This process continues until the simulation has reached the target error or lower, or it has reached the maximum number of generations. 
        
        # What to Do    
            
        In this simulation, similarly to the other evolutionary simulations, the control panel controls how the evolutionary process works and what your target qualities are. There
        will always be the default evolutionary configurations that are required for all evolutionary simulations (e.g., `Max Generations`, `Population Size`, etc.) that you can play
        with. Below are the steps to evolving the simulation:
        
        1) Specify which target quality you want by checking the box next to it.
        
        2) Specify the target value(s) that you want in the box next to it.
        
        3) After confirming all the default and target values are what you want, click on the `Evolve` button to start the simulation.
        
        4) Now, wait for the evolution process to finish, note that it can take a while depending on your configurations.

        # Credits
 
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao
        
        """.trimIndent()
        )
    }
}
