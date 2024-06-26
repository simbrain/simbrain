package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.piccolo.createTileMapLayer
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.piccolo.makeLake
import org.simbrain.util.piccolo.nextGridCoordinate
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
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

    val evaluatorParams = EvaluatorParams(
        populationSize = 100,
        eliminationRatio = 0.25,
        maxGenerations = 15,
        iterationsPerRun = 1000,
        targetValue = 1000.0,
        seed = 42
    )

    class EvolutionParameters: EditableObject {

        var useConnectionStrategyGene by GuiEditable(
            initValue = true,
            description = "Whether to use the connection gene",
            order = 20
        )

        var useLearningRuleGenes by GuiEditable(
            initValue = true,
            description = "Whether to use the local learning rule gene",
            order = 30
        )

        var useHiddenLayerUpdateRuleGene by GuiEditable(
            initValue = true,
            description = "Whether to use the hidden layer activation function gene",
            order = 40
        )


        var useLayoutGene by GuiEditable(
            initValue = true,
            description = "Whether to use the layout gene",
            order = 50
        )

    }
    val evaluationParams = EvolutionParameters()

    class EvolvePursuerPhenotype(
        val driveNeurons: NeuronCollection,
        val inputNeurons: NeuronCollection,
        val hiddenNeurons: NeuronCollection,
        val outputNeurons: NeuronCollection,
        val connections: List<Synapse>
    ) {
        val thirstNeuron get() = driveNeurons.neuronList.first()
    }

    class EvolvePursuerGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var inputChromosome = chromosome(3) {
            add(nodeGene { clamped = true })
        }
        var driveChromosome = chromosome(1) {
            add(nodeGene { clamped = true; upperBound = 100.0; lowerBound = 0.0; label = "Thirst" })
            add(nodeGene { clamped = true; upperBound = 100.0; lowerBound = 0.0 })
        }
        var hiddenChromosome = chromosome(2) { add(nodeGene()) }
        var outputChromosome = chromosome(3) { add(nodeGene { upperBound = 10.0; lowerBound = -10.0 }) }
        var connectionChromosome = chromosome(1) {
            repeat(3) {
                add(connectionGene(inputChromosome.sampleOne(), hiddenChromosome.sampleOne()))
                add(connectionGene(hiddenChromosome.sampleOne(), outputChromosome.sampleOne()))
            }
            val thirstGene = driveChromosome.first()
            add(connectionGene(thirstGene, hiddenChromosome.sampleOne()))
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
            val hiddenNeurons = NeuronCollection(network.express(hiddenChromosome)).also {
                network.addNetworkModel(it);
            }
            val outputNeurons = NeuronCollection(network.express(outputChromosome)).also {
                network.addNetworkModel(it); it.label = "outputs"
            }

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
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            // Mutate learning rule
            if (evaluationParams.useLearningRuleGenes) {
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
            if (evaluationParams.useLayoutGene) {
                layoutChromosome.forEach {
                    it.mutateParam()
                    it.mutateType()
                }
            }

            // Mutate connection strategy
            if (evaluationParams.useConnectionStrategyGene) {
                connectionStrategyChromosome.forEach {
                    it.mutateParam()
                    it.mutateType()
                }
            }

            // Mutate update rule
            if (evaluationParams.useHiddenLayerUpdateRuleGene) {
                hiddenUpdateRuleChromosome.forEach {
                    it.mutateParam(mutateBounds = false)
                    it.mutateStandardTypes()
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
        fun OdorWorld.makeRandomLake(size: IntRange = 2..8) = with(tileMap) {
            makeLake(randomTileCoordinate(), random.nextInt(size), random.nextInt(size), getLayer("Lake Layer"))
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
                thirstNeuron.activation += 10.0 / evaluatorParams.iterationsPerRun
                fitness = calories - thirstNeuron.activation * 4
            }
        }


        // What to do when a cow finds water
        workspace.addUpdateAction("water found") {
            val thirstNeuron = phenotype.await().thirstNeuron
            val odorWorld = (workspace.componentList.first { it is OdorWorldComponent } as OdorWorldComponent).world
            with(odorWorld) {
                val centerLakeSensor = evolvedAgent.sensors.first { it is TileSensor && it.label == "Center Lake Sensor" } as TileSensor
                val lakeLayer = tileMap.getLayer("Lake Layer")
                centerLakeSensor.let { sensor ->
                    // Water found
                    if (sensor.currentValue > 0.5) {
                        // Reset thirst
                        thirstNeuron.activation = 0.0
                        // Drink the sugar water
                        calories += 100.0
                        // Relocate the lake
                        tileMap.clear(lakeLayer)
                        with(simState) {
                            makeRandomLake(2..8)
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
        val lakeLayer = odorWorld.tileMap.run {
            addLayer(createTileMapLayer("Lake Layer"))
        }

        val evolvedAgent = OdorWorldEntity(odorWorld, EntityType.LION).also {
            odorWorld.addEntity(it)
            it.location = point(100, 100)
        }

        // Water sensors that can guide the agent
        val sensors = List(3) { index ->
            TileSensor("water", radius = 60.0, angle = (index * 120.0)).apply {
                decayFunction.dispersion = 250.0
            }.also {
                evolvedAgent.addSensor(it)
            }
        }

        // Central water sensor to determine when water is actually found.
        val centerLakeSensor = TileSensor("water", radius = 0.0).apply {
            decayFunction.dispersion = EntityType.LION.imageWidth / 1.4
            label = "Center Lake Sensor"
        }.also { evolvedAgent.addSensor(it) }

        init {
            with(simState) {
                odorWorld.makeRandomLake()
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
                        hiddenNeurons.location = point(0, 90)
                        outputNeurons.location = point(0, -70)
                    }

                    val energyTextObject = NetworkTextObject(simState.generateEnergyText())
                    networkComponent.network.addNetworkModels(energyTextObject)
                    workspace.addUpdateAction("update energy text") {
                        energyTextObject.text = simState.generateEnergyText()
                    }
                    energyTextObject.location = point(-160, -20)
                    withGui {
                        place(networkComponent, 5, 375, 340, 430)
                        place(odorWorldComponent, 340, 10, 800, 900)
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
        val propertyEditor = AnnotatedPropertyEditor(evaluationParams)
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
                val hiddenNeurons = network.getModels<NeuronCollection>().first { it.label?.startsWith("Layout") == true }
                val outputNeurons = network.getModelByLabel<NeuronCollection>("outputs")
                val connections = network.getModels<Synapse>().toList()

                val energyTextObject = network.getModels<NetworkTextObject>().first()

                val phenotype = CompletableDeferred(EvolvePursuerPhenotype(driveNeurons, inputNeurons, hiddenNeurons, outputNeurons, connections))

                val odorWorldComponent = workspace.componentList
                    .filterIsInstance<OdorWorldComponent>()
                    .first()

                val odorWorld = odorWorldComponent.world

                val evolvedAgent = odorWorld.entityList.first { it.entityType == EntityType.LION }

                addActions(workspace, phenotype, evolvedAgent, simState)

                workspace.addUpdateAction("update energy text") {
                    energyTextObject.text = simState.generateEnergyText()
                }

                withGui {
                    place(networkComponent, 5, 375, 340, 430)
                    place(odorWorldComponent, 340, 10, 800, 900)
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
        evaluationParams.useLayoutGene = options.optBoolean("useLayoutGene", evaluationParams.useLayoutGene)
        evaluationParams.useLearningRuleGenes = options.optBoolean("useLearningRuleGenes", evaluationParams.useLearningRuleGenes)
        evaluationParams.useConnectionStrategyGene = options.optBoolean("useConnectionStrategyGene", evaluationParams.useConnectionStrategyGene)
        evaluationParams.useHiddenLayerUpdateRuleGene = options.optBoolean("useHiddenLayerUpdateRuleGene", evaluationParams.useHiddenLayerUpdateRuleGene)
        runSim()
    }

}
