package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.loadWorkspaceZipFromFileChooser
import org.simbrain.util.piccolo.createTileMapLayer
import org.simbrain.util.piccolo.fillRect
import org.simbrain.util.piccolo.nextGridCoordinate
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.TileSensor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextInt

/**
 *  A model of resource pursuit, in which fitness was defined by ability to efficiently find food sources. Food sources
 *  are changed randomly, and fitness is penalized by energy expenditure.
 *
 *  Optimal agents should be able to develop circuits for finding food sources but spend as little energy as possible
 *  in doing so
 *
 * Run headless using:
 *  `gradle runSim -PsimName="Evolve Resource Pursuer" -PoptionString='{"maxGenerations": 15, "targetValue": 5000, "useLayoutGene": false}'`
 *
 *  for a full list of options, search for optionString in this file
 *
 *  The resulting zip file must be loaded using the `load file` button in this sim
 */
val evolveResourcePursuer = newSim { optionString ->

    val foodTileId = 574
    val foodTileType = "flower"

    val evaluatorParams = EvaluatorParams(
        populationSize = 100,
        eliminationRatio = 0.25,
        maxGenerations = 15,
        iterationsPerRun = 1000,
        targetMetric = 1000.0,
        evaluationPercentile = 5,
        seed = 42
    )

    class EvolutionParameters: EditableObject {

        var useConnectionStrategyGene by GuiEditable(
            initValue = false,
            description = "Whether to use the connection gene",
            order = 20
        )

        var useLearningRuleGenes by GuiEditable(
            initValue = false,
            description = "Whether to use the local learning rule gene",
            order = 30
        )

        var useHiddenLayerUpdateRuleGene by GuiEditable(
            initValue = false,
            description = "Whether to use the hidden layer activation function gene",
            order = 40
        )

        var useLayoutGene by GuiEditable(
            initValue = false,
            description = "Whether to use the layout gene",
            order = 50
        )

    }
    val evolutionParams = EvolutionParameters()

    class EvolvePursuerGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        val inputs by nodeChromosome(3) { clamped = true }
        val drives by nodeChromosome(2) { clamped = true; upperBound = 100.0; lowerBound = 0.0 }
        val hidden by nodeChromosome(2)
        val outputs by nodeChromosome(3) { upperBound = 10.0; lowerBound = -10.0 }
        val connections by connectionChromosome()
        val synapseRules by synapseRuleChromosome(::connections)
        val hiddenRules by neuronRuleChromosome(::hidden) { neuronRuleGene(DecayRule()) }
        val hiddenLayout by layoutChromosome(::hidden)
        val hiddenConnectionStrategy by connectionStrategyChromosome(::hidden)

        init {
            // Label the hunger drive neuron
            drives.genes[0].mutate { label = "Hunger" }

            // Initial connections
            repeat(3) {
                connections.addGene(connectionGene(inputs.genes.random(random), hidden.genes.random(random)))
                connections.addGene(connectionGene(hidden.genes.random(random), outputs.genes.random(random)))
            }
            val hungerGene = drives.genes[0]
            connections.addGene(connectionGene(hungerGene, hidden.genes.random(random)))
        }

        override fun createNew(seed: Long) = EvolvePursuerGenotype(seed)

        override fun mutate() {

            // Mutate bias
            hidden.genes.forEach {
                it.mutate {
                    bias += random.nextDouble(-.1, .1)
                }
            }

            // Mutate weights
            connections.genes.forEach {
                it.mutate {
                    strength += random.nextDouble(-.1, .1)
                }
            }

            // Mutate learning rule
            if (evolutionParams.useLearningRuleGenes) {
                synapseRules.genes.forEach {
                    it.mutateParam()
                    it.mutateType()
                }
            }

            // Add new connections (linked synapseRules auto-adds via addLinkedDefaults)
            withProbability(0.25) {
                connections.addConnection(
                    inputs to hidden,
                    drives to hidden,
                    hidden to outputs
                ) { strength = random.nextDouble(-1.0, 1.0) }
            }

            // Add a new hidden unit (linked hiddenRules auto-adds via addLinkedDefaults)
            if (random.nextDouble() < 0.5) {
                hidden.addGene(nodeGene())
            }

            // Mutate layout of hidden layer
            if (evolutionParams.useLayoutGene) {
                if (random.nextDouble() < 0.1) {
                    hiddenLayout.gene.mutateParam()
                    hiddenLayout.gene.mutateType()
                }
            }

            // Mutate connection strategy
            if (evolutionParams.useConnectionStrategyGene) {
                if (random.nextDouble() < 0.1) {
                    hiddenConnectionStrategy.gene.mutateParam()
                    hiddenConnectionStrategy.gene.mutateType()
                }
            }

            // Mutate update rule
            if (evolutionParams.useHiddenLayerUpdateRuleGene) {
                if (random.nextDouble() < 0.1) {
                    hiddenRules.genes.forEach {
                        it.mutateParam(mutateBounds = false)
                        it.mutateStandardTypes()
                    }
                }
            }

        }

    }

    data class SimState(
        var calories: Double = 400.0,
        var totalActivation: Double = 0.0,
        var movement: Double = 0.0,
        var fitness: Double = 0.0,
        val baseMetabolism: Double = 10.0,
        val seed: Long = Random.nextLong(),
        val random: Random = Random(seed)
    ) {
        fun computeCalories() = max(0.0, calories - (totalActivation + movement + baseMetabolism) * (1.0 / evaluatorParams.iterationsPerRun))
        fun OdorWorld.randomTileCoordinate() = with(tileMap) { random.nextGridCoordinate() }
        suspend fun OdorWorld.makeFoodPatch(size: IntRange = 2..8) = with(tileMap) {
            fillRect(foodTileId, randomTileCoordinate(), random.nextInt(size), random.nextInt(size), getLayer("Food Layer"))
        }
        fun generateEnergyText() = """
                            Calories: ${calories.format(2)}
                            Activation: ${totalActivation.format(2)}
                            Movement: ${movement.format(2)}
                            Fitness: ${fitness.format(2)}
                        """.trimIndent()
    }


    fun addActions(workspace: Workspace, genotype: EvolvePursuerGenotype, evolvedAgent: OdorWorldEntity, simState: SimState) {

        var calories by simState::calories
        var totalActivation by simState::totalActivation
        var movement by simState::movement
        var fitness by simState::fitness

        workspace.addUpdateAction("update energy") {
            val outputsActivations =
                genotype.outputs.neurons.activationArray.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
            val allActivations =
                (genotype.inputs.neurons.neuronList + genotype.hidden.neurons.neuronList).activations.sumOf { abs(it) } * 2
            movement = abs(evolvedAgent.speed * 3) + abs(evolvedAgent.dtheta * 2)
            totalActivation = outputsActivations + allActivations
            calories = simState.computeCalories()
            genotype.drives.neurons.neuronList.first().activation += 10.0 / evaluatorParams.iterationsPerRun
            fitness = calories - genotype.drives.neurons.neuronList.first().activation * 4
        }


        // What to do when a cow finds food
        workspace.addUpdateAction("food $foodTileType found") {
            val hungerNeuron = genotype.drives.neurons.neuronList.first()
            val odorWorld = (workspace.componentList.first { it is OdorWorldComponent } as OdorWorldComponent).world
            with(odorWorld) {
                val centerLakeSensor = evolvedAgent.sensors.first { it is TileSensor && it.label == "Center food sensor" } as TileSensor
                val lakeLayer = tileMap.getLayer("Food Layer")
                centerLakeSensor.let { sensor ->
                    // Food found
                    if (sensor.currentValue > 0.5) {
                        // Reset hunger
                        hungerNeuron.activation = 0.0
                        // Eat the food
                        calories += 100.0
                        // Relocate the lake
                        tileMap.clear(lakeLayer)
                        with(simState) {
                            makeFoodPatch(2..8)
                        }
                    }
                }
            }

        }
    }

    class EvolveResourcePursuerSim(
        val evolvePursuerGenotype: EvolvePursuerGenotype = EvolvePursuerGenotype(),
        val workspace: Workspace = Workspace(),
        seed: Long = Random.nextLong(),
    ) : EvoSim {

        val simState = SimState(
            seed = seed
        )

        val networkComponent = NetworkComponent("Network")
            .also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        private var built = false

        val odorWorldComponent = OdorWorldComponent("Odor World").also {
            workspace.addWorkspaceComponent(it)
        }
        val odorWorld = odorWorldComponent.world.apply {
            launch {
                with(tileMap) {
                    updateMapSize(24, 24)
                    fill("Grass1")
                }
            }
        }
        val foodLayer = odorWorld.tileMap.run {
            addLayer(createTileMapLayer("Food Layer"))
        }

        val evolvedAgent = runBlocking {
            OdorWorldEntity(odorWorld, EntityType.Cow).also {
                odorWorld.addEntity(it)
                it.location = point(100, 100)
            }
        }

        // Food sensors that can guide the agent
        val sensors = List(3) { index ->
            TileSensor(foodTileType, radius = 60.0, angle = (index * 120.0)).apply {
                decayFunction.dispersion = 250.0
            }.also {
                evolvedAgent.addSensor(it)
            }
        }

        // Central food sensor to determine when food is actually found.
        val centerLakeSensor = TileSensor(foodTileType, radius = 0.0).apply {
            decayFunction.dispersion = EntityType.Lion.width / 1.4
            label = "Center food sensor"
        }.also { evolvedAgent.addSensor(it) }

        init {
            workspace.launch {
                with(simState) {
                    odorWorld.makeFoodPatch()
                }
            }

            evolvedAgent.addDefaultEffectors()
            evolvedAgent.addSensor(centerLakeSensor)

            addActions(workspace, evolvePursuerGenotype, evolvedAgent, simState)
        }

        override fun mutate() {
            evolvePursuerGenotype.mutate()
        }

        override suspend fun build() {
            if (!built) {
                evolvePursuerGenotype.expressAll(network)
                evolvePursuerGenotype.drives.neurons.label = "drives"
                evolvePursuerGenotype.inputs.neurons.label = "inputs"
                evolvePursuerGenotype.inputs.neurons.neuronList.labels = listOf("Left", "Center", "Right")
                evolvePursuerGenotype.outputs.neurons.label = "outputs"
                evolvePursuerGenotype.outputs.neurons.neuronList.labels = listOf("Straight", "Left", "Right")

                with(workspace.couplingManager) {
                    sensors couple evolvePursuerGenotype.inputs.neurons.neuronList
                    evolvePursuerGenotype.outputs.neurons.neuronList couple evolvedAgent.effectors
                }
                built = true
            }
        }

        override fun visualize(workspace: Workspace): EvolveResourcePursuerSim {
            return EvolveResourcePursuerSim(evolvePursuerGenotype.copyGenotype() as EvolvePursuerGenotype, workspace)
        }

        override fun copy(): EvoSim {
            return EvolveResourcePursuerSim(evolvePursuerGenotype.copyGenotype() as EvolvePursuerGenotype, Workspace())
        }

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
            return simState.fitness
        }

    }

    fun displayBlock(genotype: EvolvePursuerGenotype): GeneDisplayBuilder.() -> Unit {
        val allNodes = genotype.inputs.genes + genotype.drives.genes +
            genotype.hidden.genes + genotype.outputs.genes
        fun nodeIndex(gene: NodeGene) = allNodes.indexOf(gene).let { if (it >= 0) it + 1 else "?" }
        return {
            display(genotype.inputs, noDefaults = true) {
                header(formatted("Node") { nodeIndex(it) })
                +template(Neuron::clamped)
            }
            display(genotype.drives, noDefaults = true) {
                header(formatted("node") { nodeIndex(it) })
                +template(Neuron::label)
            }
            display(genotype.hidden) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.outputs) {
                header(formatted("node") { nodeIndex(it) })
                +template(Neuron::upperBound)
                +template(Neuron::lowerBound)
            }
            display(genotype.connections) {
                +formatted("in") { nodeIndex(it.source) }
                +formatted("out") { nodeIndex(it.target) }
            }
            display(genotype.hiddenRules) {
                +formatted("updateRule") { it.template.updateRule.name }
            }
            display(genotype.synapseRules) {
                +formatted("learningRule") { it.template.learningRule.name }
            }
            display(genotype.hiddenLayout) {
                +template(LayoutGeneWrapper::layoutType)
                +template(LayoutGeneWrapper::hSpacing)
                +template(LayoutGeneWrapper::vSpacing)
            }
            display(genotype.hiddenConnectionStrategy) {
                +formatted("connectionStrategy") { it.template.connectionStrategy.tooltipText() }
            }
        }
    }

    suspend fun runSim() {
        val genomeDisplay = EvolvePursuerGenotype().let {
            it.geneticsDisplay(
                precision = 3,
                metricLabel = evaluatorParams.stoppingCondition.name,
                block = displayBlock(it)
            )
        }
        withGui { showGeneDisplay(genomeDisplay) }

        withContext(workspace.coroutineContext) {
            val lastGeneration = evaluator(
                evaluatorParams = evaluatorParams,
                populatingFunction = { EvolveResourcePursuerSim(seed = seed) }
            ) {
                val bestGenotype = (best as EvolveResourcePursuerSim).evolvePursuerGenotype
                genomeDisplay.refreshFrom(bestGenotype, metadata = bestMetadata, block = displayBlock(bestGenotype))
            }
            lastGeneration.take(1).forEach { best ->
                with(best.visualize(workspace) as EvolveResourcePursuerSim) {
                    build()
                    val genotype = this.evolvePursuerGenotype
                    genotype.drives.neurons.location = point(-150, 150)
                    genotype.inputs.neurons.location = point(0, 150)

                    offsetNeuronCollections(genotype.inputs.neurons, genotype.hidden.neurons, Direction.NORTH, 100.0)
                    offsetNeuronCollections(genotype.hidden.neurons, genotype.outputs.neurons, Direction.NORTH, 100.0)

                    alignNetworkModels(genotype.inputs.neurons, genotype.hidden.neurons, Alignment.VERTICAL)
                    alignNetworkModels(genotype.hidden.neurons, genotype.outputs.neurons, Alignment.VERTICAL)

                    genomeDisplay.refreshFrom(genotype, block = displayBlock(genotype))

                    val energyTextObject = NetworkTextObject(simState.generateEnergyText())
                    networkComponent.network.addNetworkModelsAsync(energyTextObject)
                    workspace.addUpdateAction("update energy text") {
                        energyTextObject.text = simState.generateEnergyText()
                    }
                    energyTextObject.location = point(-160, -20)
                    withGui {
                        place(networkComponent, 390, 10, 380, 600)
                        place(odorWorldComponent, 770, 10, 620, 600)
                        (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).worldPanel.scalingFactor = 0.5
                    }
                    if (desktop == null) {
                        workspace.save(File("evolved_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.zip"), headless = true)
                    }
                }
            }
        }

    }

    withGui {
        workspace.clearWorkspace()
        val controlPanel = evaluatorParams.createControlPanel("Control Panel", 5, 10)
        controlPanel.addSeparator()
        val propertyEditor = AnnotatedPropertyEditor(evolutionParams)
        controlPanel.addAnnotatedPropertyEditor(propertyEditor)
        evaluatorParams.addControlPanelButton("Evolve") {
            workspace.removeAllComponents()
            evaluatorParams.addProgressWindow()
            runSim()
        }
        controlPanel.addButton("Load Workspace") {
            val loadOk = loadWorkspaceZipFromFileChooser()
            if (loadOk) {

                val simState = SimState()

                val networkComponent = workspace.componentList
                    .filterIsInstance<NetworkComponent>()
                    .first()

                val network = networkComponent.network

                val driveNeurons = network.getModelByLabel<NeuronCollection>("drives")
                val inputNeurons = network.getModelByLabel<NeuronCollection>("inputs")
                val outputNeurons = network.getModelByLabel<NeuronCollection>("outputs")
                val hiddenNeurons = (network.getModels<NeuronCollection>() - setOf(driveNeurons, inputNeurons, outputNeurons))
                    .also {
                        if (it.size != 1) {
                            throw Error("Expected exactly one neuron collection that is not 'drives', 'inputs' or 'outputs', but got ${it.size}: ${it.map(NeuronCollection::name)}")
                        }
                    }
                    .first()
                val connections = network.getModels<Synapse>().toList()

                val energyTextObject = network.getModels<NetworkTextObject>().first()

                val odorWorldComponent = workspace.componentList
                    .filterIsInstance<OdorWorldComponent>()
                    .first()

                val odorWorld = odorWorldComponent.world

                val evolvedAgent = odorWorld.entityList.first { it.entityType == EntityType.Cow }

                // For load case, use the raw update actions directly
                var calories by simState::calories
                var totalActivation by simState::totalActivation
                var movement by simState::movement
                var fitness by simState::fitness

                workspace.addUpdateAction("update energy") {
                    val outputsActivations =
                        outputNeurons.activationArray.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                    val allActivations =
                        (inputNeurons.neuronList + hiddenNeurons.neuronList).activations.sumOf { abs(it) } * 2
                    movement = abs(evolvedAgent.speed * 3) + abs(evolvedAgent.dtheta * 2)
                    totalActivation = outputsActivations + allActivations
                    calories = simState.computeCalories()
                    driveNeurons.neuronList.first().activation += 10.0 / evaluatorParams.iterationsPerRun
                    fitness = calories - driveNeurons.neuronList.first().activation * 4
                }

                workspace.addUpdateAction("food $foodTileType found") {
                    val hungerNeuron = driveNeurons.neuronList.first()
                    with(odorWorld) {
                        val centerLakeSensor = evolvedAgent.sensors.first { it is TileSensor && it.label == "Center food sensor" } as TileSensor
                        val lakeLayer = tileMap.getLayer("Food Layer")
                        centerLakeSensor.let { sensor ->
                            if (sensor.currentValue > 0.5) {
                                hungerNeuron.activation = 0.0
                                calories += 100.0
                                tileMap.clear(lakeLayer)
                                with(simState) {
                                    makeFoodPatch(2..8)
                                }
                            }
                        }
                    }
                }

                workspace.addUpdateAction("update energy text") {
                    energyTextObject.text = simState.generateEnergyText()
                }

                withGui {
                    place(networkComponent, 390, 10, 380, 600)
                    place(odorWorldComponent, 770, 10, 620, 600)
                    (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).worldPanel.scalingFactor = 0.5
                }
            }
        }
    }

    addSidebarInfo(
        """
        # Evolving A Resource Pursuer

        This is a simulation of the evolution of a neural network that is coupled to an agent in an odor world that contains food resources. The neural network will evolve
        to optimize its foraging strategy within the limitations of caloric expenditure and gain using an evolutionary framework in Simbrain.

        # Simulation Details

        This simulation simulates the evolution of a neural network until the `target fitness` in the control panel is met, exceeded, or when it has reached the `maximum generation`. The
        goal of this simulation is to evolve until it is as close as possible to the `target fitness`.

        In simple terms, the fitness is calculated as `calories(t) - hunger(t)` where `calories` is calculated as `totalActivation(t) + Movement(t)`.

        For a more in-depth look into how the fitness is calculated, use this [page](https://docs.simbrain.net/docs/simulations/) as a guide to see the simulation code. Whereas for a comprehensive
        look into how evolutionary simulations are developed in Simbrain, look [here](https://docs.simbrain.net/docs/evolution/). To see example simulations that were made during a SURF project,
        see [here](https://tbmvthao.github.io/SampleEvosims/).

        ## Evolutionary Process

        The evolutionary process begins with a starting `population size` of simulations. In generation `0`, each simulation starts with a three-layer network (`3` input neurons, `2` hidden neurons, and `3` output
        neurons), a `Hunger` neuron, and a preset amount of connections (`3` per layer). Within each generation, a simulation will iterate until the specified value while the fitness of each simulation is calculated
        and recorded. As a simulation iterates, `400` calories are added when the agent obtains food and the `Hunger` neuron's activation is increased by `10` at each iteration. Hunger will reset to `0` when the agent
        obtains food and new food is repopulated at a random location in the odor world.

        After each generation, a percentage of the population is eliminated (e.g., `elimination ratio`) and repopulated with new simulations. During this process of reproduction, some of the new simulations will have
        mutations, where the simulation develops new connections between neuron layers (`25%` chance), new neurons in the hidden layer (`50%` chance), changes in neuron biases and weight strengths. If the any of the
        other genes are active, they will also produce other possible mutations:

        1) Changes in `learning rule` for weights (`10%` chance)

        2) Changes in `connection strategy` (`10%` chance)

        3) Changes in `update rule` for hidden neurons (`10%` chance)

        4) Changes in the `layout` of the hidden layer (`10%` chance)

        Then a percentage of the top performers is evaluated (e.g, `Evaluation percentile`) to determine if the `target fitness` has been achieved. This process continues until the simulation has reached the
        `target fitness` or better, or when the evolutionary process ends.

        # What to Do

        In this simulation, similar to the other evolutionary simulations, the control panel controls how the evolutionary process works. As mentioned before, there are genes that can be activated to allow
        the simulation to have additional mutations. Click on the dropdown box next to the gene you want active, and change it to `Yes`. Below are the steps to evolving the simulation:

        1) Specify the parameters of the simulation.

            - The addition of more mutations.

        2) After confirming the parameters are what you want, click on the `Evolve` button to start the simulation.

        3) Now, wait for the evolution process to finish, note that it can take a while depending on your configurations.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        Kanly Thao

        """.trimIndent()
    )

    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)
        evaluatorParams.maxGenerations = options.optInt("maxGenerations", evaluatorParams.maxGenerations)
        evaluatorParams.targetMetric = options.optDouble("targetMetric", evaluatorParams.targetMetric)
        evaluatorParams.iterationsPerRun = options.optInt("iterationsPerRun", evaluatorParams.iterationsPerRun)
        evaluatorParams.populationSize = options.optInt("populationSize", evaluatorParams.populationSize)
        evaluatorParams.eliminationRatio = options.optDouble("eliminationRatio", evaluatorParams.eliminationRatio)
        evaluatorParams.evalutationPercentile = options.optInt("evaluationPercentile", evaluatorParams.evalutationPercentile)
        evolutionParams.useLayoutGene = options.optBoolean("useLayoutGene", evolutionParams.useLayoutGene)
        evolutionParams.useLearningRuleGenes = options.optBoolean("useLearningRuleGenes", evolutionParams.useLearningRuleGenes)
        evolutionParams.useConnectionStrategyGene = options.optBoolean("useConnectionStrategyGene", evolutionParams.useConnectionStrategyGene)
        evolutionParams.useHiddenLayerUpdateRuleGene = options.optBoolean("useHiddenLayerUpdateRuleGene", evolutionParams.useHiddenLayerUpdateRuleGene)
        runSim()
    }

}
