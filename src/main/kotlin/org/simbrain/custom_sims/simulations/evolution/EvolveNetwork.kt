package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.core.bound
import org.simbrain.network.core.lengths
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

    class EvolveNetworkGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        val nodes by nodeChromosome(2) { upperBound = 10.0; lowerBound = -10.0 }
        val connections by connectionChromosome()
        val layout by layoutChromosome(::nodes)

        init {
            connections.addConnection(nodes to nodes) {
                strength = random.nextDouble(-1.0, 1.0)
            }
        }

        override fun createNew(seed: Long) = EvolveNetworkGenotype(seed)

        override fun mutate() {
            // Mutate layout
            layout.gene.mutateParam()
            layout.gene.mutateType()

            // Add nodes
            if (random.nextDouble() < 0.1) {
                nodes.addGene(nodeGene())
            }

            // Mutate biases
            nodes.genes.forEach {
                it.mutate {
                    bias += random.nextDouble(-1.0, 1.0)
                }
            }

            // Add new connections
            withProbability(0.25) {
                connections.addConnection(
                    nodes to nodes
                ) { strength = random.nextDouble(-1.0, 1.0) }
            }

            // Mutate strengths
            connections.genes.forEach {
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

        private var built = false

        override fun mutate() {
            evolveNetworkGenotype.mutate()
        }

        override suspend fun build() {
            if (!built) {
                evolveNetworkGenotype.expressAll(network)
                built = true
            }
        }

        override fun visualize(workspace: Workspace): EvolveNetworkSim {
            return EvolveNetworkSim(evolveNetworkGenotype.copyGenotype() as EvolveNetworkGenotype, workspace)
        }

        override fun copy(): EvoSim {
            return EvolveNetworkSim(evolveNetworkGenotype.copyGenotype() as EvolveNetworkGenotype, Workspace())
        }

        override suspend fun eval(): Double {
            build()

            // Iterate network to stabilize network
            repeat(evaluatorParams.iterationsPerRun) { network.bufferedUpdate() }

            // TODO: Normalize errors, provide for weightings, and consider squared error

            var totalError = 0.0

            // Number of nodes
            if (networkParams.useNumNodes) {
                totalError += abs(evolveNetworkGenotype.nodes.neurons.neuronList.size - networkParams.targetNumNodes)
            }

            // Num Weights
            if (networkParams.useNumWeights) {
                totalError += abs(evolveNetworkGenotype.connections.synapses.size - networkParams.targetNumWeights)
            }

            // Average activation
            if (networkParams.useAverageActivation) {
                totalError += abs(evolveNetworkGenotype.nodes.neurons.neuronList.activations.average() - networkParams.targetAverageActivation)
            }

            // Total Activation
            if (networkParams.useTotalActivation) {
                totalError += abs(evolveNetworkGenotype.nodes.neurons.neuronList.activations.sum() - networkParams.targetTotalActivation)
            }

            // Average length of connections
            if (networkParams.useAverageConnectionLength) {
                totalError += abs(evolveNetworkGenotype.connections.synapses.lengths.average() - networkParams.targetAverageConnectionLength)
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

    fun displayBlock(genotype: EvolveNetworkGenotype): GeneDisplayBuilder.() -> Unit {
        val allNodes = genotype.nodes.genes
        fun nodeIndex(gene: NodeGene) = allNodes.indexOf(gene).let { if (it >= 0) it + 1 else "?" }
        return {
            display(genotype.nodes) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.connections, noDefaults = true) {
                +formatted("in") { nodeIndex(it.source) }
                +formatted("out") { nodeIndex(it.target) }
                +template(Synapse::strength)
            }
            display(genotype.layout) {
                +template(LayoutGeneWrapper::layoutType)
                +template(LayoutGeneWrapper::hSpacing)
                +template(LayoutGeneWrapper::vSpacing)
            }
        }
    }

    suspend fun runSim() {
        val genomeDisplay = EvolveNetworkGenotype().let {
            it.geneticsDisplay(metricLabel = evaluatorParams.stoppingCondition.name, block = displayBlock(it))
        }
        withGui { showGeneDisplay(genomeDisplay) }

        val lastGeneration = evaluator(
            evaluatorParams,
            populatingFunction = { EvolveNetworkSim(EvolveNetworkGenotype(seed = seed)) }
        ) {
            val bestGenotype = (best as EvolveNetworkSim).evolveNetworkGenotype
            genomeDisplay.refreshFrom(bestGenotype, metadata = bestMetadata, block = displayBlock(bestGenotype))
        }
        lastGeneration.take(1).forEach {
            with(it.visualize(workspace) as EvolveNetworkSim) {
                build()
                genomeDisplay.refreshFrom(evolveNetworkGenotype, block = displayBlock(evolveNetworkGenotype))
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
        # Evolving A Network

        This is a simulation of the evolution of a network towards a or, multiple target qualities using an evolutionary framework in Simbrain.

        # Simulation Details

        This simulation simulates the evolution of a neural network until the `target error` in the control panel is met, exceeded, or when it has reached the `maximum generation`.
        The goal of this simulation is to evolve until it is as close as possible to the `target error`.

        The `target error` is calculated as the total sum error of the target qualities (e.g., `Target Num of Nodes` `Target Num of Weights`, etc). Each target quality error is
        calculated as the difference between current active quality vs active target quality.

        The target qualities are explained in the tooltip. To see their explanations, hover over them in the control panel. For a comprehensive look into how evolutionary simulations
        are developed in Simbrain.

        ## Evolutionary Process

        The evolutionary process begins with a starting `population size` of simulations. In generation `0`, each simulation starts with `2` neurons. Within each generation, the simulation will iterate until
        the specified value while the fitness of each simulation is calculated and recorded.

        Then after each generation, a percentage of the population is eliminated (e.g., `elimination ratio`) and repopulated with new simulations. During this process of reproduction, some of the new simulations
        will have mutations, where the simulation develops new connections between neuron layers (`25%` chance), new hidden neurons (`10%` chance), changes in neurons biases and weight strengths, and the physical layout
        of the neurons.

        After each generation, a percentage of the top performers is evaluated (e.g, `Evaluation percentile`) to determine if the `target error` has been achieved. This process continues until the simulation has reached the
        `target error` or lower, or when the evolutionary process ends.

        # What to Do

        In this simulation, similar to the other evolutionary simulations, the control panel controls how the evolutionary process works and what your target qualities are.
        Below are the steps to evolving the simulation:

        1) Specify the parameters of the simulation.

            - The addition of more target qualities.

            - Setting the target value(s).

        2) After confirming all the default and target values are what you want, click on the `Evolve` button to start the simulation.

        3) Now, wait for the evolution process to finish, note that it can take a while depending on your configurations.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Kanly Thao

        """.trimIndent()
        )
    }
}
