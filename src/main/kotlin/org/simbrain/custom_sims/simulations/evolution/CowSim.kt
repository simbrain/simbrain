package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.util.cartesianProduct
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.piccolo.createTileMapLayer
import org.simbrain.util.piccolo.makeLake
import org.simbrain.util.piccolo.nextGridCoordinate
import org.simbrain.util.point
import org.simbrain.util.sampleOne
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.TileSensor
import java.awt.Dimension
import kotlin.random.Random


// See CowGrazing.kt


val evolveCow = newSim {

    val maxGenerations = 50
    val iterationsPerRun = 2000

    class CowGenotype(seed: Long = Random.nextLong()) : Genotype(seed) {
        val inputs by nodeChromosome(3) { clamped = true }
        val hidden by nodeChromosome(2)
        val outputs by nodeChromosome(3) { upperBound = 10.0; lowerBound = -10.0 }
        val drives by nodeChromosome(1) { activation = 10.0; upperBound = 10.0; clamped = true }
        val connections by connectionChromosome()

        init {
            repeat(3) {
                connections.chromosome.add(connectionGene(inputs.genes.random(random), hidden.genes.random(random)))
                connections.chromosome.add(connectionGene(hidden.genes.random(random), outputs.genes.random(random)))
            }
            connections.chromosome.add(connectionGene(drives.genes[0], hidden.genes.random(random)))
        }

        override fun createNew(seed: Long) = CowGenotype(seed)

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

            val existingPairs = connections.genes.map { it.source to it.target }.toSet()
            val availableConnections = ((inputs.chromosome + hidden.chromosome + outputs.chromosome) cartesianProduct (hidden.chromosome + outputs.chromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne(random)
                connections.chromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            val availablePairs = (drives.chromosome cartesianProduct (inputs.chromosome + hidden.chromosome + outputs.chromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availablePairs.isNotEmpty()) {
                val (source, target) = availablePairs.sampleOne(random)
                connections.chromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Make hidden layer larger
            if (random.nextDouble() < 0.1) {
                hidden.addGene(nodeGene())
            }
        }
    }

    class CowGroupGenotype private constructor(
        seed: Long,
        val cows: List<CowGenotype>
    ) : Genotype(seed) {

        constructor(seed: Long = Random.nextLong(), numCows: Int = 2) : this(
            seed, List(numCows) { CowGenotype(Random(seed).nextLong()) }
        )

        override fun mutate() = cows.forEach { it.mutate() }

        override fun createNew(seed: Long) = CowGroupGenotype(seed, cows.size)

        override fun copyGenotype() = CowGroupGenotype(
            random.nextLong(),
            cows.map { it.copyGenotype() as CowGenotype }
        )
    }

    class CowSim(
        genotype: CowGroupGenotype = CowGroupGenotype(),
        workspace: Workspace = Workspace(),
        metadata: SimMetadata? = null
    ) : EvoSim<CowGroupGenotype>(genotype, workspace, metadata) {

        val cowGenotypes get() = genotype.cows

        val thirstThreshold = 5.0
        val cowFitnesses = mutableMapOf<Int, Double>()

        fun randomTileCoordinate() = with(odorWorld.tileMap) { genotype.random.nextGridCoordinate() }
        private val lakeSize
            get() = genotype.random.nextInt(2, 8)

        val odorWorld = OdorWorldComponent("Odor World").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            launch {
                with(tileMap) {
                    updateMapSize(32, 32)
                    fill("Grass1")
                }
            }
        }
        val lakeLayer = odorWorld.tileMap.run {
            addLayer(createTileMapLayer("Lake Layer"))
        }
        val networks = List(cowGenotypes.size) { index ->
            NetworkComponent("Network ${index + 1}").also { workspace.addWorkspaceComponent(it) }.network
        }
        val entities = runBlocking {
            List(cowGenotypes.size) { i ->
                OdorWorldEntity(odorWorld, EntityType.Cow).also {
                    odorWorld.addEntity(it)
                    it.location = point((i + 1) * 100, (i + 1) * 100)
                }
            }
        }
        // Water sensors that can guide the cow
        val sensors = entities.map { entity ->
            List(3) { index ->
                TileSensor("water", radius = 60.0, angle = (index * 120.0)).apply {
                    decayFunction.dispersion = 250.0
                }.also { entity.addSensor(it) }
            }
        }
        // Central water sensor to determine when water is actually found.
        val centerLakeSensors = entities.associateWith { entity ->
            TileSensor("water", radius = 0.0).apply {
                decayFunction.dispersion = entity.width / 1.4
            }.also { entity.addSensor(it) }
        }
        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }

        fun addUpdateActions(cow: CowGenotype, cowIndex: Int, entity: OdorWorldEntity) {

            val thirstNeuron = cow.drives.neurons.neuronList.first()

            fun addFitness(fitnessDelta: Double) {
                cowFitnesses[cowIndex] = (cowFitnesses[cowIndex] ?: 0.0) + fitnessDelta
            }

            // What to do when a cow finds water
            workspace.addUpdateAction("water found") {
                with(odorWorld.tileMap) {
                    centerLakeSensors[entity]?.let { sensor ->
                        // Water found
                        if (sensor.currentValue > 0.5) {
                            // Reset thirst node
                            thirstNeuron.activation = 0.0
                            // Relocate the lake
                            clear(lakeLayer)
                            val newLocation = randomTileCoordinate()
                            makeLake(newLocation, lakeSize, lakeSize, lakeLayer)
                        }
                    }
                }
            }

            // Update thirst and fitness
            workspace.addUpdateAction("update thirst") {
                thirstNeuron.activation = thirstNeuron.activation + 0.005
                addFitness(-thirstNeuron.activation)
            }

            // Impose a fitness cost for motion and increase thirst with motion
            workspace.addUpdateAction("update energy") {
                val energy = (entity.speed * entity.speed) / (iterationsPerRun * 2)
            }
        }

        override suspend fun onBuild() {
            // Make the lake
            with(odorWorld.tileMap) {
                List(1) { randomTileCoordinate() }.forEach {
                    makeLake(it, lakeSize, lakeSize, lakeLayer)
                }
            }
            // Express the genotypes
            cowGenotypes.zip(networks).forEach { (genotype, network) ->
                genotype.expressAll(network)
            }
            // Make couplings
            with(workspace.couplingManager) {
                cowGenotypes.indices.forEach { i ->
                    sensors[i] couple cowGenotypes[i].inputs.neurons.neuronList
                    cowGenotypes[i].outputs.neurons.neuronList couple effectors[i]
                }
            }
            // Add update actions
            cowGenotypes.indices.forEach { i ->
                cowFitnesses[i] = 0.0
                addUpdateActions(cowGenotypes[i], i, entities[i])
            }
        }

        override fun create(genotype: CowGroupGenotype, workspace: Workspace, metadata: SimMetadata?) =
            CowSim(genotype, workspace, metadata)

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(iterationsPerRun)
            // Determine a fitness for the sim based on the fitness of each cow
            return cowFitnesses.values.min()
        }
    }

    workspace.launch {
        val progressWindow = ProgressWindow(maxGenerations, "Fitness").apply {
            minimumSize = Dimension(300, 100)
            setLocationRelativeTo(null)
        }
        val runner = EvolutionRunner(
            populatingFunction = { CowSim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            stoppingFunction = { nthPercentileFitness(10) > -1000 || generation > maxGenerations },
        )
        runner.events.generationUpdated.on { state ->
            listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                "$it: ${state.nthPercentileFitness(it).format(3)}"
            }.also {
                println("[${state.generation}] $it")
                progressWindow.text = "5th Percentile Fitness: ${state.nthPercentileFitness(10).format(3)}"
                progressWindow.value = state.generation
            }
        }
        runner.events.endEvolution.on {
            runner.generationState?.let { state ->
                with(state.best.createDisplayCopy(workspace) as CowSim) {
                    build()
                    cowGenotypes.forEach { g ->
                        g.inputs.neurons.location = point(0, 150)
                        g.hidden.neurons.location = point(0, 60)
                        g.outputs.neurons.location = point(0, -25)
                        g.drives.neurons.location = point(200, 60)
                    }
                }
            }
            progressWindow.close()
        }
        runner.startEvolving()
    }
}
