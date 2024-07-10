package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.network.util.*
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.piccolo.createTileMapLayer
import org.simbrain.util.piccolo.fillRect
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.piccolo.nextGridCoordinate
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

    class EvolvePursuerPhenotype(
        val driveNeurons: NeuronCollection,
        val inputNeurons: NeuronCollection,
        val hiddenNeurons: NeuronCollection,
        val outputNeurons: NeuronCollection,
        val connections: List<Synapse>
    ) {
        val hungerNeuron get() = driveNeurons.neuronList.first()
    }

    class EvolvePursuerGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var inputChromosome = chromosome(3) {
            add(nodeGene { clamped = true })
        }
        var driveChromosome = chromosome(1) {
            add(nodeGene { clamped = true; upperBound = 100.0; lowerBound = 0.0; label = "Hunger" })
            add(nodeGene { clamped = true; upperBound = 100.0; lowerBound = 0.0 })
        }
        var hiddenChromosome = chromosome(2) { add(nodeGene()) }
        var outputChromosome = chromosome(3) { add(nodeGene { upperBound = 10.0; lowerBound = -10.0 }) }
        var connectionChromosome = chromosome(1) {
            repeat(3) {
                add(connectionGene(inputChromosome.sampleOne(), hiddenChromosome.sampleOne()))
                add(connectionGene(hiddenChromosome.sampleOne(), outputChromosome.sampleOne()))
            }
            val hungerGene = driveChromosome.first()
            add(connectionGene(hungerGene, hiddenChromosome.sampleOne()))
        }
        var synapseRuleChromosome = chromosome(connectionChromosome.size) {
            add(synapseRuleGene())
        }
        var layoutChromosome = chromosome(1) {
            add(layoutGene())
        }
        var connectionStrategyChromosome = chromosome(1) {
            add(connectionStrategyGene())
        }
        var hiddenUpdateRuleChromosome = chromosome(hiddenChromosome.size) {
            add(neuronRuleGene(DecayRule()))
        }

        suspend fun expressWith(network: Network): EvolvePursuerPhenotype {
            val driveNeurons = NeuronCollection(network.express(driveChromosome)).also {
                network.addNetworkModel(it); it.label = "drives"
            }
            val inputNeurons = NeuronCollection(network.express(inputChromosome)).also {
                network.addNetworkModel(it); it.label = "inputs"
            }
            inputNeurons.neuronList.labels = listOf("Left", "Center", "Right")

            val hiddenNeurons = NeuronCollection(network.express(hiddenChromosome)).also {
                network.addNetworkModel(it);
            }
            val outputNeurons = NeuronCollection(network.express(outputChromosome)).also {
                network.addNetworkModel(it); it.label = "outputs"
            }
            outputNeurons.neuronList.labels = listOf("Straight", "Left", "Right")

            val connections = network.express(connectionChromosome)
            val layout = express(layoutChromosome).first().express()
            layout.layoutNeurons(hiddenNeurons.neuronList)

            val synapseRules = express(synapseRuleChromosome)
            connections.zip(synapseRules).forEach { (connection, rule) ->
                connection.learningRule = rule.learningRule
            }

            val connectionStrategyWrapper = express(connectionStrategyChromosome).first()
            connectionStrategyWrapper.connectionStrategy.connectNeurons(
                hiddenNeurons.neuronList,
                hiddenNeurons.neuronList
            ).addToNetwork(network)
            hiddenNeurons.label = "${connectionStrategyWrapper.connectionStrategy}"

            val hiddenUpdateRules = express(hiddenUpdateRuleChromosome)
            hiddenNeurons.neuronList.zip(hiddenUpdateRules).forEach { (neuron, rule) ->
                neuron.updateRule = rule.updateRule
            }

            return EvolvePursuerPhenotype(driveNeurons, inputNeurons, hiddenNeurons, outputNeurons, connections)
        }

        fun copy() = EvolvePursuerGenotype(random.nextLong()).apply {
            val current = this@EvolvePursuerGenotype
            val new = this@apply

            new.driveChromosome = current.driveChromosome.copy()
            new.inputChromosome = current.inputChromosome.copy()
            new.hiddenChromosome = current.hiddenChromosome.copy()
            new.outputChromosome = current.outputChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
            new.synapseRuleChromosome = current.synapseRuleChromosome.copy()
            new.layoutChromosome = current.layoutChromosome.copy()
            new.connectionStrategyChromosome = current.connectionStrategyChromosome.copy()
            new.hiddenUpdateRuleChromosome = current.hiddenUpdateRuleChromosome.copy()
        }

        fun mutate() {

            // Mutate bias
            hiddenChromosome.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-.1, .1)
                    }
                }
            }

            // Mutate weights
            connectionChromosome.forEach {
                it.mutate {
                    strength +=  random.nextDouble(-.1, .1)
                }
            }

            // Mutate learning rule
            if (evolutionParams.useLearningRuleGenes) {
                synapseRuleChromosome.forEach {
                    it.mutateParam()
                    it.mutateType()
                }
            }

            // Add new connections
            val newConnectionGene = withProbability(0.25) {
                connectionChromosome.createGene(
                    inputChromosome + driveChromosome to hiddenChromosome,
                    hiddenChromosome to outputChromosome
                ) { strength = random.nextDouble(-1.0, 1.0) }
            }
            if (newConnectionGene != null) {
                synapseRuleChromosome.add(synapseRuleGene())
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.5) {
                hiddenChromosome.add(nodeGene())
                hiddenUpdateRuleChromosome.add(neuronRuleGene(DecayRule()))
            }

            // Mutate layout of hidden layer
            if (evolutionParams.useLayoutGene) {
                if (random.nextDouble() < 0.1) {
                    layoutChromosome.forEach {
                        it.mutateParam()
                        it.mutateType()
                    }
                }
            }

            // Mutate connection strategy
            if (evolutionParams.useConnectionStrategyGene) {
                if (random.nextDouble() < 0.1) {
                    connectionStrategyChromosome.forEach {
                        it.mutateParam()
                        it.mutateType()
                    }
                }
            }

            // Mutate update rule
            if (evolutionParams.useHiddenLayerUpdateRuleGene) {
                if (random.nextDouble() < 0.1) {
                    hiddenUpdateRuleChromosome.forEach {
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
        fun OdorWorld.makeFoodPatch(size: IntRange = 2..8) = with(tileMap) {
            fillRect(foodTileId, randomTileCoordinate(), random.nextInt(size), random.nextInt(size), getLayer("Food Layer"))
        }
        fun generateEnergyText() = """
                            Calories: ${calories.format(2)}
                            Activation: ${totalActivation.format(2)}
                            Movement: ${movement.format(2)}
                            Fitness: ${fitness.format(2)}
                        """.trimIndent()
    }


    fun addActions(workspace: Workspace, phenotype: Deferred<EvolvePursuerPhenotype>, evolvedAgent: OdorWorldEntity, simState: SimState) {

        var calories by simState::calories
        var totalActivation by simState::totalActivation
        var movement by simState::movement
        var fitness by simState::fitness

        workspace.addUpdateAction("update energy") {
            with(phenotype.await()) {
                val outputsActivations =
                    outputNeurons.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                val allActivations =
                    (inputNeurons.neuronList + hiddenNeurons.neuronList).activations.sumOf { abs(it) } * 2
                movement = abs(evolvedAgent.speed * 3) + abs(evolvedAgent.dtheta * 2)
                totalActivation = outputsActivations + allActivations
                calories = simState.computeCalories()
                hungerNeuron.activation += 10.0 / evaluatorParams.iterationsPerRun
                fitness = calories - hungerNeuron.activation * 4
            }
        }


        // What to do when a cow finds food
        workspace.addUpdateAction("food $foodTileType found") {
            val hungerNeuron = phenotype.await().hungerNeuron
            val odorWorld = (workspace.componentList.first { it is OdorWorldComponent } as OdorWorldComponent).world
            with(odorWorld) {
                val centerLakeSensor = evolvedAgent.sensors.first { it is TileSensor && it.label == "Center Food Sensor" } as TileSensor
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

        val phenotypeDeferred = CompletableDeferred<EvolvePursuerPhenotype>()

        val odorWorldComponent = OdorWorldComponent("Odor World").also {
            workspace.addWorkspaceComponent(it)
        }
        val odorWorld = odorWorldComponent.world.apply {
            loadTileMap("empty.tmx")
            with(tileMap) {
                updateMapSize(24, 24)
                fill("Grass1")
            }
        }
        val foodLayer = odorWorld.tileMap.run {
            addLayer(createTileMapLayer("Food Layer"))
        }

        val evolvedAgent = OdorWorldEntity(odorWorld, EntityType.COW).also {
            odorWorld.addEntity(it)
            it.location = point(100, 100)
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
            decayFunction.dispersion = EntityType.LION.imageWidth / 1.4
            label = "Center Food Sensor"
        }.also { evolvedAgent.addSensor(it) }

        init {
            with(simState) {
                odorWorld.makeFoodPatch()
            }

            evolvedAgent.addDefaultEffectors()
            evolvedAgent.addSensor(centerLakeSensor)

            addActions(workspace, phenotypeDeferred, evolvedAgent, simState)
        }

        override fun mutate() {
            evolvePursuerGenotype.mutate()
        }

        override suspend fun build() {
            if (!phenotypeDeferred.isCompleted) {
                // Express the genotypes
                phenotypeDeferred.complete(evolvePursuerGenotype.expressWith(network))
                // Make couplings
                val agent = phenotypeDeferred.await()
                with(workspace.couplingManager) {
                    sensors couple agent.inputNeurons.neuronList
                    agent.outputNeurons.neuronList couple evolvedAgent.effectors
                }
            }
        }

        override fun visualize(workspace: Workspace): EvolveResourcePursuerSim {
            return EvolveResourcePursuerSim(evolvePursuerGenotype.copy(), workspace)
        }

        override fun copy(): EvoSim {
            return EvolveResourcePursuerSim(evolvePursuerGenotype.copy(), Workspace())
        }

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
            return simState.fitness
        }

    }

    suspend fun runSim() {
        withContext(workspace.coroutineContext) {
            val lastGeneration = evaluator(
                evaluatorParams = evaluatorParams,
                populatingFunction = { EvolveResourcePursuerSim(seed = seed) }
            )
            lastGeneration.take(1).forEach {
                with(it.visualize(workspace) as EvolveResourcePursuerSim) {
                    build()
                    val phenotype = this.phenotypeDeferred.await()
                    phenotype.apply {
                        driveNeurons.location = point(-150, 150)
                        inputNeurons.location = point(0, 150)

                        offsetNeuronCollections(inputNeurons, hiddenNeurons, Direction.NORTH, 100.0)
                        offsetNeuronCollections(hiddenNeurons, outputNeurons, Direction.NORTH, 100.0)

                        alignNetworkModels(inputNeurons, hiddenNeurons, Alignment.VERTICAL)
                        alignNetworkModels(hiddenNeurons, outputNeurons, Alignment.VERTICAL)
                    }

                    val energyTextObject = NetworkTextObject(simState.generateEnergyText())
                    networkComponent.network.addNetworkModels(energyTextObject)
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

                val phenotype = CompletableDeferred(EvolvePursuerPhenotype(driveNeurons, inputNeurons, hiddenNeurons, outputNeurons, connections))

                val odorWorldComponent = workspace.componentList
                    .filterIsInstance<OdorWorldComponent>()
                    .first()

                val odorWorld = odorWorldComponent.world

                val evolvedAgent = odorWorld.entityList.first { it.entityType == EntityType.COW }

                addActions(workspace, phenotype, evolvedAgent, simState)

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
