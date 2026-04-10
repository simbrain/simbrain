package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.labels
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getRandomLocation
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Run headless using:
 * `gradle runSim -PsimName="Evolve mouse pursuer" -PoptionString='{"useEnergyModel": false}'`
 */
val evolveMousePursuer = newSim { optionString ->

    val evaluatorParams = EvaluatorParams(
        populationSize = 100,
        eliminationRatio = 0.25,
        maxGenerations = 20,
        iterationsPerRun = 1000,
        targetMetric = 80.0,
        evaluationPercentile = 10,
        seed = 42
    )

    class MouseEvolutionParameters : EditableObject {

        var useEnergyModel by GuiEditable(
            initValue = true,
            description = "Penalize movement and activation while replenishing energy when cheese is found",
            order = 0
        )

        var numCheeses by GuiEditable(
            initValue = 1,
            description = "Number of cheese targets present during each evaluation",
            min = 1,
            order = 10
        )

        var cheeseReward by GuiEditable(
            initValue = 25.0,
            description = "Fitness gained when the mouse catches a cheese",
            min = 0.0,
            order = 20
        )

        var movementPenaltyScale by GuiEditable(
            initValue = 1.0,
            description = "Multiplier on energy cost due to movement",
            min = 0.0,
            order = 30
        )

        var activationPenaltyScale by GuiEditable(
            initValue = 0.3,
            description = "Multiplier on energy cost due to network activation",
            min = 0.0,
            order = 40
        )

        var startingCalories by GuiEditable(
            initValue = 200.0,
            description = "Initial calorie budget when energy mode is enabled",
            min = 0.0,
            order = 50
        )

        var caloriesPerCheese by GuiEditable(
            initValue = 50.0,
            description = "Calories restored when cheese is found in energy mode",
            min = 0.0,
            order = 60
        )

        var baseMetabolism by GuiEditable(
            initValue = 2.0,
            description = "Per-step baseline metabolic cost in energy mode",
            min = 0.0,
            order = 70
        )
    }

    val mouseParams = MouseEvolutionParameters()

    class MouseGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        val inputs by nodeChromosome(3) { clamped = true }
        val hidden by nodeChromosome(1)
        val outputs by nodeChromosome(3)
        val connections by connectionChromosome()

        init {
            // Configure individual input genes
            inputs.genes[0].mutate { label = "Cheese Left" }
            inputs.genes[1].mutate { label = "Cheese Right" }
            inputs.genes[2].mutate { label = "Hunger"; lowerBound = 0.0; upperBound = 1.0 }

            // Configure hidden gene
            hidden.genes[0].mutate { label = "Hidden 1" }

            // Configure output genes
            outputs.genes[0].mutate { label = "Speed"; lowerBound = -10.0; upperBound = 10.0; bias = 0.5 }
            outputs.genes[1].mutate { label = "Left"; lowerBound = -200.0; upperBound = 200.0 }
            outputs.genes[2].mutate { label = "Right"; lowerBound = -200.0; upperBound = 200.0 }

            // Braitenberg scaffold with a single internal hunger input.
            connections.addGene(connectionGene(inputs.genes[0], outputs.genes[1]) { strength = 10.0 })
            connections.addGene(connectionGene(inputs.genes[1], outputs.genes[2]) { strength = 10.0 })
            connections.addGene(connectionGene(inputs.genes[0], outputs.genes[0]) { strength = 1.5 })
            connections.addGene(connectionGene(inputs.genes[1], outputs.genes[0]) { strength = 1.5 })
            connections.addGene(connectionGene(inputs.genes[2], outputs.genes[0]) { strength = 1.5 })
            connections.addGene(connectionGene(inputs.genes[2], hidden.genes[0]) { strength = 0.5 })
            connections.addGene(connectionGene(hidden.genes[0], outputs.genes[1]) { strength = 0.25 })
            connections.addGene(connectionGene(hidden.genes[0], outputs.genes[2]) { strength = 0.25 })
        }

        override fun createNew(seed: Long) = MouseGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach {
                it.mutate {
                    bias += random.nextDouble(-0.2, 0.2)
                }
            }

            outputs.genes.forEach {
                it.mutate {
                    bias += random.nextDouble(-0.1, 0.1)
                }
            }

            connections.genes.forEach {
                it.mutate {
                    strength += random.nextDouble(-0.5, 0.5)
                }
            }

            val existingPairs = connections.genes.map { it.source to it.target }.toSet()
            val availablePairs =
                (inputs.genes + hidden.genes).flatMap { source ->
                    (hidden.genes + outputs.genes).map { target -> source to target }
                } - existingPairs
            if (random.nextDouble() < 0.25 && availablePairs.isNotEmpty()) {
                val (source, target) = availablePairs.random(random)
                connections.addGene(connectionGene(source, target) {
                    strength = random.nextDouble(-1.0, 1.0)
                })
            }

            if (random.nextDouble() < 0.15) {
                val newHidden = nodeGene { label = "Hidden ${hidden.genes.size + 1}" }
                hidden.addGene(newHidden)
                connections.addGene(connectionGene(inputs.genes.random(random), newHidden) {
                    strength = random.nextDouble(-1.0, 1.0)
                })
                connections.addGene(connectionGene(newHidden, outputs.genes.random(random)) {
                    strength = random.nextDouble(-1.0, 1.0)
                })
            }
        }
    }

    data class SimState(
        var calories: Double = mouseParams.startingCalories,
        var fitness: Double = 0.0,
        var cheesesFound: Int = 0,
        var movement: Double = 0.0,
        var activationCost: Double = 0.0,
        var hunger: Double = 0.0
    ) {
        fun summary(useEnergyModel: Boolean) = buildString {
            appendLine("Cheeses: $cheesesFound")
            appendLine("Fitness: ${fitness.format(2)}")
            appendLine("Hunger: ${hunger.format(2)}")
            if (useEnergyModel) {
                appendLine("Calories: ${calories.format(2)}")
                appendLine("Movement: ${movement.format(2)}")
                append("Activation: ${activationCost.format(2)}")
            } else {
                append("Energy model: off")
            }
        }
    }

    class EvolveMousePursuerSim(
        val genotype: MouseGenotype = MouseGenotype(),
        val workspace: Workspace = Workspace(),
        seed: Long = Random.nextLong()
    ) : EvoSim {

        private val random = Random(seed)
        val simState = SimState()

        val networkComponent = NetworkComponent("Network").also(workspace::addWorkspaceComponent)
        val network = networkComponent.network

        private var built = false

        val odorWorldComponent = OdorWorldComponent("Odor World").also(workspace::addWorkspaceComponent)
        val odorWorld = odorWorldComponent.world.apply {
            launch {
                isObjectsBlockMovement = true
                wrapAround = true
                tileMap.updateMapSize(18, 18)
                tileMap.fill("Grass1")
            }
        }

        val mouse = runBlocking {
            odorWorld.addEntity(120, 120, EntityType.Mouse).apply {
                heading = random.nextDouble(0.0, 360.0)
                removeAllSensors()
                removeAllEffectors()
                addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0).apply {
                    label = "Cheese Left"
                    decayFunction.dispersion = 160.0
                })
                addSensor(ObjectSensor(EntityType.Swiss, 50.0, -45.0).apply {
                    label = "Cheese Right"
                    decayFunction.dispersion = 160.0
                })
                addDefaultEffectors()
            }
        }

        val cheeses = mutableListOf<OdorWorldEntity>()

        init {
            workspace.launch {
                repeat(mouseParams.numCheeses) {
                    cheeses += spawnCheese()
                }
            }

            mouse.events.collided.on { collided ->
                if (collided is OdorWorldEntity && collided.entityType == EntityType.Swiss) {
                    simState.cheesesFound += 1
                    simState.fitness += mouseParams.cheeseReward
                    if (mouseParams.useEnergyModel) {
                        simState.calories += mouseParams.caloriesPerCheese
                    }
                    simState.hunger = 0.0
                    mouse.isEffectorsEnabled = true
                    respawnCheese(collided)
                }
            }

            addUpdateActions()
        }

        private suspend fun spawnCheese(): OdorWorldEntity {
            val location = odorWorld.getRandomLocation()
            return odorWorld.addEntity(location.x.toInt(), location.y.toInt(), EntityType.Swiss).apply {
                name = "Cheese ${cheeses.size + 1}"
            }
        }

        private fun respawnCheese(cheese: OdorWorldEntity, minSeparation: Double = 100.0) {
            val otherEntities = odorWorld.entityList.filter { it !== cheese }
            var newLocation = odorWorld.getRandomLocation()
            while (otherEntities.any { it.location.distance(newLocation) < minSeparation }) {
                newLocation = odorWorld.getRandomLocation()
            }
            cheese.location = point(newLocation.x, newLocation.y)
        }

        private fun addUpdateActions() {
            workspace.addUpdateAction("update mouse fitness") {
                val speed = abs(mouse.speed)
                val turning = abs(mouse.dtheta)
                val outputActivity = genotype.outputs.neurons.neuronList.sumOf { abs(it.activation) }
                val hiddenActivity = genotype.hidden.neurons.neuronList.sumOf { abs(it.activation) }

                simState.movement = speed + turning
                simState.activationCost = outputActivity + hiddenActivity

                if (mouseParams.useEnergyModel) {
                    val movementCost =
                        simState.movement * mouseParams.movementPenaltyScale / evaluatorParams.iterationsPerRun
                    val activationCost =
                        simState.activationCost * mouseParams.activationPenaltyScale / evaluatorParams.iterationsPerRun
                    val basalCost = mouseParams.baseMetabolism / evaluatorParams.iterationsPerRun
                    val totalCost = movementCost + activationCost + basalCost
                    simState.calories = (simState.calories - totalCost).coerceAtLeast(0.0)
                    val calorieBaseline = mouseParams.startingCalories.coerceAtLeast(1.0)
                    simState.hunger = (1.0 - (simState.calories / calorieBaseline)).coerceIn(0.0, 1.0)
                    simState.fitness -= totalCost
                    if (simState.calories <= 0.0) {
                        mouse.isEffectorsEnabled = false
                        mouse.speed = 0.0
                        mouse.dtheta = 0.0
                        genotype.outputs.neurons.neuronList.forEach { it.activation = 0.0 }
                    } else {
                        mouse.isEffectorsEnabled = true
                    }
                } else {
                    simState.hunger = 0.0
                    mouse.isEffectorsEnabled = true
                    val shapingReward = (speed * 0.01) / evaluatorParams.iterationsPerRun
                    simState.fitness += shapingReward
                }

                genotype.inputs.neurons.neuronList[2].activation = simState.hunger
            }
        }

        override fun mutate() {
            genotype.mutate()
        }

        override suspend fun build() {
            if (!built) {
                genotype.expressAll(network)
                genotype.inputs.neurons.neuronList.labels = listOf("Cheese Left", "Cheese Right", "Hunger")
                genotype.outputs.neurons.neuronList.labels = listOf("Speed", "Left", "Right")
                with(workspace.couplingManager) {
                    mouse.sensors.filterIsInstance<ObjectSensor>() couple genotype.inputs.neurons.neuronList.take(2)
                    genotype.outputs.neurons.neuronList couple mouse.effectors
                }
                built = true
            }
            genotype.inputs.neurons.neuronList[2].activation = simState.hunger
        }

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
            return simState.fitness
        }

        override fun visualize(workspace: Workspace): EvoSim {
            return EvolveMousePursuerSim(genotype.copyGenotype() as MouseGenotype, workspace, random.nextLong())
        }

        override fun copy(): EvoSim {
            return EvolveMousePursuerSim(genotype.copyGenotype() as MouseGenotype, Workspace(), random.nextLong())
        }

        suspend fun showWinner() {
            build()
            genotype.inputs.neurons.neuronList.forEachIndexed { index, neuron ->
                neuron.location = point(index * 120.0, 180.0)
            }
            genotype.hidden.neurons.neuronList.forEachIndexed { index, neuron ->
                neuron.location = point(index * 120.0, 80.0)
            }
            genotype.outputs.neurons.neuronList.forEachIndexed { index, neuron ->
                neuron.location = point(index * 120.0, -20.0)
            }

            val energyTextObject = NetworkTextObject(simState.summary(mouseParams.useEnergyModel))
            network.addNetworkModelsAsync(energyTextObject)
            energyTextObject.location = point(-150, -40)
            workspace.addUpdateAction("update mouse status text") {
                energyTextObject.text = simState.summary(mouseParams.useEnergyModel)
            }

            withGui {
                place(networkComponent, 390, 10, 380, 600)
                place(odorWorldComponent, 770, 10, 620, 600)
                (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).worldPanel.scalingFactor = 0.6
            }

            if (desktop == null) {
                workspace.save(
                    File("evolved_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.zip"),
                    headless = true
                )
            }
        }
    }

    fun displayBlock(genotype: MouseGenotype): GeneDisplayBuilder.() -> Unit {
        val allNodes = genotype.inputs.genes + genotype.hidden.genes + genotype.outputs.genes
        fun nodeIndex(gene: NodeGene) = allNodes.indexOf(gene).let { if (it >= 0) it + 1 else "?" }
        return {
            display(genotype.inputs, noDefaults = true) {
                header(formatted("node") { nodeIndex(it) })
                +template(Neuron::label)
            }
            display(genotype.hidden) {
                header(formatted("node") { nodeIndex(it) })
            }
            display(genotype.outputs, noDefaults = true) {
                header(formatted("node") { nodeIndex(it) })
                +template(Neuron::label)
            }
            display(genotype.connections) {
                +formatted("in") { nodeIndex(it.source) }
                +formatted("out") { nodeIndex(it.target) }
            }
        }
    }

    suspend fun runEvolution() {
        withContext(workspace.coroutineContext) {
            val genomeDisplay = MouseGenotype().let {
                it.geneticsDisplay(metricLabel = evaluatorParams.stoppingCondition.name, block = displayBlock(it))
            }
            withGui { showGeneDisplay(genomeDisplay) }

            val lastGeneration = evaluator(
                evaluatorParams = evaluatorParams,
                populatingFunction = { EvolveMousePursuerSim(seed = evaluatorParams.seed.toLong()) }
            ) {
                evaluatorParams.updateProgressWindow(this)
                val bestGenotype = (best as EvolveMousePursuerSim).genotype
                genomeDisplay.refreshFrom(bestGenotype, metadata = bestMetadata, block = displayBlock(bestGenotype))
            }

            lastGeneration.take(1).forEach {
                val sim = it.visualize(workspace) as EvolveMousePursuerSim
                genomeDisplay.refreshFrom(sim.genotype, block = displayBlock(sim.genotype))
                sim.showWinner()
            }
        }
    }

    withGui {
        workspace.clearWorkspace()
        val controlPanel = evaluatorParams.createControlPanel("Control Panel", 5, 10)
        val propertyEditor = AnnotatedPropertyEditor(mouseParams)
        controlPanel.addAnnotatedPropertyEditor(propertyEditor)
        evaluatorParams.addControlPanelButton("Evolve") {
            workspace.removeAllComponents()
            evaluatorParams.addProgressWindow()
            propertyEditor.commitChanges()
            runEvolution()
        }
    }

    addSidebarInfo(
        """
        # Evolve Mouse Pursuer

        This simulation evolves a mouse in [OdorWorld](https://docs.simbrain.net/docs/worlds/odorworld.html) to pursue cheese starting from a simple Braitenberg-style controller.

        # Simulation Details

        The initial controller uses only left and right cheese sensors coupled directly to speed and turning outputs. A single hunger input neuron provides an internal state signal derived from calorie depletion. Evolution begins from that scaffold and can then change weights, add connections, and grow hidden units.

        The `useEnergyModel` toggle allows direct comparison between two selection pressures:

        1. Energy on: finding cheese must offset movement and activation costs.
        2. Energy off: the same body and movement system are used, but fitness is driven primarily by successful pursuit.

        Cheese capture is detected through collision logic rather than a center steering sensor, so the controller remains close to the original Braitenberg setup. When eaten, cheese respawns at a new location away from the mouse and other objects, following the same pursuit pattern used in the Braitenberg reinforcement learning simulation.

        # What to Do

        1. Set evolutionary parameters in the control panel.
        2. Turn `useEnergyModel` on or off depending on the comparison you want.
        3. Click `Evolve` and inspect the best evolved mouse and network when the run completes.
        """.trimIndent(),
        width = 300
    )

    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)
        evaluatorParams.populationSize = options.optInt("populationSize", evaluatorParams.populationSize)
        evaluatorParams.eliminationRatio = options.optDouble("eliminationRatio", evaluatorParams.eliminationRatio)
        evaluatorParams.iterationsPerRun = options.optInt("iterationsPerRun", evaluatorParams.iterationsPerRun)
        evaluatorParams.maxGenerations = options.optInt("maxGenerations", evaluatorParams.maxGenerations)
        evaluatorParams.targetMetric = options.optDouble("targetMetric", evaluatorParams.targetMetric)
        evaluatorParams.evalutationPercentile =
            options.optInt("evaluationPercentile", evaluatorParams.evalutationPercentile)
        evaluatorParams.seed = options.optInt("seed", evaluatorParams.seed)

        mouseParams.useEnergyModel = options.optBoolean("useEnergyModel", mouseParams.useEnergyModel)
        mouseParams.numCheeses = options.optInt("numCheeses", mouseParams.numCheeses)
        mouseParams.cheeseReward = options.optDouble("cheeseReward", mouseParams.cheeseReward)
        mouseParams.movementPenaltyScale =
            options.optDouble("movementPenaltyScale", mouseParams.movementPenaltyScale)
        mouseParams.activationPenaltyScale =
            options.optDouble("activationPenaltyScale", mouseParams.activationPenaltyScale)
        mouseParams.startingCalories = options.optDouble("startingCalories", mouseParams.startingCalories)
        mouseParams.caloriesPerCheese = options.optDouble("caloriesPerCheese", mouseParams.caloriesPerCheese)
        mouseParams.baseMetabolism = options.optDouble("baseMetabolism", mouseParams.baseMetabolism)
    }
}
