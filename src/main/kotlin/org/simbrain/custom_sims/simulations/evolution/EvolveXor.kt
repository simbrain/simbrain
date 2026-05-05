package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.activations
import org.simbrain.util.StandardDialog
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

    class XorGenotype(seed: Long = Random.nextLong()) : Genotype(seed) {

        val inputs by nodeChromosome(2) { clamped = true; upperBound = 1.0; lowerBound = -1.0 }
        val hidden by nodeChromosome(2) { upperBound = 1.0; lowerBound = -1.0 }
        val output by nodeChromosome(1) { upperBound = 1.0; lowerBound = -1.0 }
        val connections by connectionChromosome()

        init {
            connections.addConnection(inputs to hidden) {
                strength = random.nextDouble(-1.0, 1.0)
            }
            connections.addConnection(hidden to output) {
                strength = random.nextDouble(-1.0, 1.0)
            }
        }

        override fun createNew(seed: Long) = XorGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach {
                it.mutate {
                    bias += random.nextDouble(-1.0, 1.0)
                }
            }

            connections.genes.forEach {
                it.mutate {
                    strength += random.nextDouble(-1.0, 1.0)
                }
            }

            withProbability(0.25) {
                connections.addConnection(
                    inputs to hidden,
                    hidden to output
                ) { strength = random.nextDouble(-1.0, 1.0) }
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.1) {
                hidden.addGene(nodeGene())
            }
        }
    }

    class XorSim(
        genotype: XorGenotype = XorGenotype(),
        workspace: Workspace = Workspace(),
        metadata: SimMetadata? = null
    ) : EvoSim<XorGenotype>(genotype, workspace, metadata) {

        val networkComponent = NetworkComponent("${metadata.namePrefix}network").also {
            workspace.addWorkspaceComponent(it)
        }

        val network = networkComponent.network

        override suspend fun onBuild() {
            genotype.expressAll(network)
            genotype.inputs.neurons.label = "input"
            genotype.hidden.neurons.label = "hidden"
            genotype.output.neurons.label = "output"
        }

        override fun create(genotype: XorGenotype, workspace: Workspace, metadata: SimMetadata?) =
            XorSim(genotype, workspace, metadata)

        override suspend fun eval(): Double {
            build()
            val testData = listOf(
                listOf(0.0, 0.0) to listOf(0.0),
                listOf(0.0, 1.0) to listOf(1.0),
                listOf(1.0, 0.0) to listOf(1.0),
                listOf(1.0, 1.0) to listOf(0.0)
            )

            return testData.sumOf { (input, output) ->
                genotype.inputs.neurons.neuronList.activations = input
                // Iterate more each run if allowing recurrent connections
                workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
                val error = (genotype.output.neurons.neuronList.activations sse output)
                error
            }
        }

    }

    fun displayBlock(genotype: XorGenotype): GeneDisplayBuilder.() -> Unit {
        val allNodes = genotype.inputs.genes + genotype.hidden.genes + genotype.output.genes
        fun nodeIndex(gene: NodeGene) = allNodes.indexOf(gene).let { if (it >= 0) it + 1 else "?" }
        return {
            display(genotype.inputs) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.hidden) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.output) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.connections) {
                +formatted("in") { nodeIndex(it.source) }
                +formatted("out") { nodeIndex(it.target) }
            }
        }
    }

    val controlPanel = EvolutionControlPanel(evaluatorParams)

    var trainerDialog: StandardDialog? = null
    var session: EvolutionTrainerSession? = null

    withGui {
        workspace.clearWorkspace()
        controlPanel.show(this, "Control Panel", 5, 10, addParamsEditor = false)

        workspace.events.workspaceCleared.on(Dispatchers.Swing) {
            trainerDialog?.dispose()
            trainerDialog = null
            session = null
        }

        suspend fun openTrainer() {
            trainerDialog?.takeIf { it.isDisplayable }?.let {
                it.toFront()
                return
            }

            val activeSession = session ?: run {
                workspace.removeAllComponents()
                val runner = EvolutionRunner(evaluatorParams) { seed -> XorSim(XorGenotype(seed = seed)) }
                val genomeDisplay = geneDisplayPanel(displayBlock = ::displayBlock)
                genomeDisplay.bind(runner)
                val history = ExpressionHistory()

                suspend fun expressBest() {
                    val state = runner.generationState ?: return
                    history.minimizeAll()
                    with(state.best.createDisplayCopy(workspace, state.bestMetadata) as XorSim) {
                        build()
                        val genotype = this.genotype
                        genotype.inputs.neurons.neuronList.forEach { it.increment = 1.0 }
                        genotype.inputs.neurons.location = point(0, 150)
                        genotype.hidden.neurons.location = point(0, 60)
                        genotype.output.neurons.location = point(0, -25)
                        genomeDisplay.refreshFrom(genotype)
                        withGui {
                            place(networkComponent, 340, 10, 384, 480)
                        }
                        history.add(ExpressionEntry.forComponents(
                            workspace, listOf(networkComponent), state.historyLabel(evaluatorParams)
                        ))
                    }
                }

                runner.events.targetReached.on(Dispatchers.Default) { expressBest() }

                EvolutionTrainerSession(
                    runner = runner,
                    evaluatorParams = evaluatorParams,
                    extras = listOf(genomeDisplay),
                    onExpress = ::expressBest,
                    history = history
                ).also { session = it }
            }

            trainerDialog = createEvolutionTrainerDialog(activeSession).apply {
                addCloseTask { trainerDialog = null }
                makeVisible()
            }
        }

        controlPanel.addButton("Open Trainer") { openTrainer() }

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
