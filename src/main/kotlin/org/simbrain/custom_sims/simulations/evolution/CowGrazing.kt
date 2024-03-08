package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.*
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getRandomLocation
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

val grazingCows = newSim { optionString ->

    var numCows = 2
    var maxGenerations = 50
    var iterationsPerRun = 2000
    var populationSize = 100
    var eliminationRatio = .5
    var numFlowers = 10
    // If not, use min to compute group level fitness across cows
    var useAverage = false

    class CowGenotype(seed: Long = Random.nextLong()) : Genotype {
        override val random: Random = Random(seed)
        var inputChromosome = chromosome(1) {
            // Dandelion and cow sensors
            repeat(6) {
                add(nodeGene { clamped = true })
            }
            // Won't get coupled to. Serves as an initial "drive" neuron
            add(nodeGene { clamped = true; activation = 1.0 })
        }
        var hiddenChromosome = chromosome(2) { add(nodeGene()) }
        var outputChromosome = chromosome(3) { add(nodeGene { upperBound = 10.0; lowerBound = -10.0 }) }
        var connectionChromosome = chromosome(1) {
            repeat(3) {
                add(connectionGene(inputChromosome.sampleOne(), hiddenChromosome.sampleOne()))
                add(connectionGene(hiddenChromosome.sampleOne(), outputChromosome.sampleOne()))
            }
            // Force an initial "drive"
            add(connectionGene(inputChromosome[3], hiddenChromosome.sampleOne()))
        }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network.express(inputChromosome)).also {
                    network.addNetworkModelAsync(it); it.label = "input"
                },
                NeuronCollection(network.express(hiddenChromosome)).also {
                    network.addNetworkModelAsync(it); it.label = "hidden"
                },
                NeuronCollection(network.express(outputChromosome)).also {
                    network.addNetworkModelAsync(it); it.label = "output"
                },
                network.express(connectionChromosome)
            )
        }

        fun copy() = CowGenotype(random.nextLong()).apply {
            val current = this@CowGenotype
            val new = this@apply
            new.inputChromosome = current.inputChromosome.copy()
            new.hiddenChromosome = current.hiddenChromosome.copy()
            new.outputChromosome = current.outputChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
        }

        fun mutate() {
            hiddenChromosome.forEach {
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

            val existingPairs = connectionChromosome.map { it.source to it.target }.toSet()
            val availableConnections =
                ((inputChromosome + hiddenChromosome + outputChromosome) cartesianProduct (hiddenChromosome + outputChromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne(random)
                connectionChromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Make hidden layer larger
            if (random.nextDouble() < 0.1) {
                hiddenChromosome.add(nodeGene())
            }
        }
    }

    // What to do when a cow finds flower
    fun addFindFlowerAction(workspace: Workspace, entity: OdorWorldEntity, fitnessLambda: (Double) -> Unit = {}) {
        val world = entity.world
        workspace.addUpdateAction("${entity.name} found a flower") {
            (entity.getSensor("centralFlowerSensor") as ObjectSensor).let { sensor ->
                // Flowers found
                sensor.getSensedObjects(entity, .5).forEach {
                    it.location = world.getRandomLocation()
                    // Update fitness
                    fitnessLambda(1.0)
                }
            }

        }
    }


    class CowSim(
        val cowGenotypes: List<CowGenotype> = List(numCows) { CowGenotype() },
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val random = Random(cowGenotypes.first().random.nextInt())

        val cowFitnesses = mutableMapOf<CowGenotype.Phenotype, Double>()

        private val _cowPhenotypes = CompletableDeferred<List<CowGenotype.Phenotype>>()
        val cowPhenotypes: Deferred<List<CowGenotype.Phenotype>> get() = _cowPhenotypes

        val odorWorld = OdorWorldComponent("Odor World 1").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            isObjectsBlockMovement = false
            loadTileMap("empty.tmx")
            with(tileMap) {
                updateMapSize(25, 25)
                fill("Grass1")
            }
        }

        val networks = List(cowGenotypes.size) { index ->
            NetworkComponent("Network ${index + 1}").also { workspace.addWorkspaceComponent(it) }.network
        }
        val entities = List(cowGenotypes.size) { i ->
            OdorWorldEntity(odorWorld, EntityType.COW).also {
                odorWorld.addEntity(it)
                it.location = point((i + 1) * 100, (i + 1) * 100)
            }
        }

        val dandelionSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.DANDELIONS, radius = 60.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 250.0
                }.also { entity.addSensor(it) }
            }
        }

        val cowSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.COW, radius = 50.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 200.0
                }.also { entity.addSensor(it) }
            }
        }


        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }

        init {
            // Central flower sensor to determine when the flower is actually found.
            entities.forEach{
                it.addSensor(
                    ObjectSensor(EntityType.DANDELIONS, radius = 0.0).apply {
                        label = "centralFlowerSensor"
                        decayFunction = StepDecayFunction()
                        decayFunction.dispersion = 30.0
                    }
                )
            }
            repeat(numFlowers) {
                val loc = odorWorld.getRandomLocation()
                odorWorld.addEntity(loc.x.toInt(), loc.y.toInt(),
                    EntityType.DANDELIONS, doubleArrayOf(1.0))
            }
            workspace.launch {
                (cowPhenotypes.await() zip entities).forEach { (phenotype, entity) ->
                    addUpdateActions(phenotype, entity)
                }
            }
        }

        fun addUpdateActions(cow: CowGenotype.Phenotype, entity: OdorWorldEntity) {

            fun addFitness(fitnessDelta: Double) {
                cowFitnesses[cow] = (cowFitnesses[cow] ?: 0.0) + fitnessDelta
            }
            addFitness(0.0) // To initialize fitness

            addFindFlowerAction(workspace, entity) { addFitness(1.0) }
        }

        override suspend fun build() {
            if (!_cowPhenotypes.isCompleted) {
                // Express the genotypes
                _cowPhenotypes.complete(
                    cowGenotypes.zip(networks).map { (genotype, network) -> genotype.expressWith(network) })
                // Make couplings
                with(workspace.couplingManager) {
                    val cows = _cowPhenotypes.await()
                    (0..cows.lastIndex).map { i ->
                        val cow = cows[i]
                        (dandelionSensors[i] + cowSensors[i]) couple cow.inputs.neuronList
                        cow.outputs.neuronList couple effectors[i]
                    }
                }
            }
        }

        override fun mutate() {
            cowGenotypes.forEach { it.mutate() }
        }

        override fun visualize(workspace: Workspace) = CowSim(cowGenotypes.map { it.copy() }, workspace)

        override fun copy() = CowSim(cowGenotypes.map { it.copy() }, Workspace())

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(iterationsPerRun)
            // Determine a fitness for the sim based on the fitness of each cow
            return if (useAverage) {
                cowFitnesses.values.average()
            } else {
                cowFitnesses.values.minOrNull() ?: 0.0
            }
        }
    }

    suspend fun runSim() {
        withContext(workspace.coroutineContext) {
            val progressWindow = withGui {
                ProgressWindow(maxGenerations, "10th Percentile Fitness:").apply {
                    minimumSize = Dimension(300, 100)
                    setLocationRelativeTo(null)
                }
            }
            val cowSims = evaluator(
                populatingFunction = { CowSim() },
                populationSize = populationSize,
                eliminationRatio = eliminationRatio,
                stoppingFunction = {
                    nthPercentileFitness(10) > 400 || generation > maxGenerations
                },
                peek = {
                    listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                        "$it: ${nthPercentileFitness(it).format(3)}"
                    }.also {
                        println("[$generation] $it")
                        progressWindow?.apply {
                            text = "10th Percentile Fitness: ${nthPercentileFitness(10).format(3)}"
                            value = generation
                        }
                    }
                }
            )
            cowSims.take(1).forEach {
                with(it.visualize(workspace) as CowSim) {
                    build()
                    withGui {
                        workspace.componentList.filterIsInstance<OdorWorldComponent>().first().apply {
                            place(this, 280, 10, 476, 432)
                        }
                        workspace.componentList.filterIsInstance<NetworkComponent>().forEachIndexed { i, net ->
                            place(net, 768, 10 + i * 282, 326, 282)
                        }
                    }
                    cowPhenotypes.await().forEach {
                        it.inputs.location = point(0, 150)
                        it.hiddens.location = point(0, 60)
                        it.outputs.location = point(0, -25)
                    }
                    if (desktop == null) {
                        workspace.save(File("evolved_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.zip"), headless = true)
                    }
                }
            }
            progressWindow?.close()
        }
    }

    withGui {
        workspace.clearWorkspace()
        createControlPanel("Control Panel", 5, 10) {

            val numCowsTf = addTextField("Number of cows", "" + numCows)
            val maxGenTf = addTextField("Max Generations", "" + maxGenerations)
            val iterationsPerRunTf = addTextField("Num iterations per generation", "" + iterationsPerRun)
            val populationSizeTf = addTextField("Population size", "" + populationSize)
            val eliminationRatioTf = addTextField("Elimination ratio", "" + eliminationRatio)
            val useAverageCB = addCheckBox("Use mean group fitness (else min)", useAverage)

            addButton("Evolve") {
                workspace.removeAllComponents()
                numCows = numCowsTf.text.toInt();
                maxGenerations = maxGenTf.text.toInt()
                iterationsPerRun = iterationsPerRunTf.text.toInt()
                populationSize = populationSizeTf.text.toInt()
                eliminationRatio = eliminationRatioTf.text.toDouble()
                useAverage = useAverageCB.isSelected()
                runSim()
            }

            addButton("Load file") {
                val simulationChooser = SFileChooser(workspace.currentDirectory, "Zip Archive", "zip")
                val simFile = simulationChooser.showOpenDialog()
                val serializer = WorkspaceSerializer(workspace)
                if (simFile != null) {
                    workspace.removeAllComponents()
                    workspace.updater.updateManager.reset()
                    withContext(Dispatchers.IO) {
                        serializer.deserialize(FileInputStream(simFile))
                    }
                }

                val world = workspace.componentList.filterIsInstance<OdorWorldComponent>().first().world
                world.entityList
                    .filter { e -> e.entityType == EntityType.COW }
                    .forEach { addFindFlowerAction(workspace, it) }
            }
        }
    }
    if (optionString?.isNotEmpty() == true) {
        val options = optionString.split(":")
        numCows = options[0].toInt()
        maxGenerations = options[1].toInt()
        iterationsPerRun = options[2].toInt()
        populationSize = options[3].toInt()
        eliminationRatio = options[4].toDouble()
        if (options.size > 5) {
            useAverage = options[5].toBoolean()
        }
        runSim()
    }

}