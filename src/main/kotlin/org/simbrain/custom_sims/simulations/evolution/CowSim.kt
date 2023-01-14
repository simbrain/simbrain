package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.cartesianProduct
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm2.*
import org.simbrain.util.piccolo.createTileMapLayer
import org.simbrain.util.piccolo.loadTileMap
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


val evolveCow = newSim {

    val maxGenerations = 50
    val iterationsPerRun = 2000

    class CowGenotype(seed: Long = Random.nextLong()) : Genotype2 {
        override val random: Random = Random(seed)
        var inputChromosome = chromosome2(3) { add(nodeGene2 { isClamped = true }) }
        var hiddenChromosome = chromosome2(2) { add(nodeGene2()) }
        var outputChromosome = chromosome2(3) { add(nodeGene2 { upperBound = 10.0; lowerBound = -10.0 }) }
        var driveChromosome = chromosome2(1) { add(nodeGene2 { activation = 10.0; upperBound = 10.0; isClamped = true
        }) }
        var connectionChromosome = chromosome2(1) {
            repeat(3) {
                add(connectionGene2(inputChromosome.sampleOne(), hiddenChromosome.sampleOne()))
                add(connectionGene2(hiddenChromosome.sampleOne(), outputChromosome.sampleOne()))
            }
            add(connectionGene2(driveChromosome[0], hiddenChromosome.sampleOne()))
        }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val drives: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network, network.express(inputChromosome)).also {
                    network.addNetworkModel(it); it.label = "input"
                },
                NeuronCollection(network, network.express(hiddenChromosome)).also {
                    network.addNetworkModel(it); it.label = "hidden"
                },
                NeuronCollection(network, network.express(outputChromosome)).also {
                    network.addNetworkModel(it); it.label = "output"
                },
                NeuronCollection(network, network.express(driveChromosome)).also {
                    network.addNetworkModel(it); it.label = "drives"
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
            new.driveChromosome = current.driveChromosome.copy()
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
            val availableConnections = ((inputChromosome + hiddenChromosome + outputChromosome) cartesianProduct (hiddenChromosome + outputChromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne(random)
                connectionChromosome.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            val availablePairs = (driveChromosome cartesianProduct (inputChromosome + hiddenChromosome + outputChromosome)) - existingPairs
            if (random.nextDouble() < 0.25 && availablePairs.isNotEmpty()) {
                val (source, target) = availablePairs.sampleOne(random)
                connectionChromosome.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Make hidden layer larger
            if (random.nextDouble() < 0.1) {
                hiddenChromosome.add(nodeGene2())
            }
        }
    }

    class CowSim(
        val cowGenotypes: List<CowGenotype> = List(2) { CowGenotype() },
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val random = Random(cowGenotypes.first().random.nextInt())

        val thirstThreshold = 5.0
        val cowFitnesses = mutableMapOf<CowGenotype.Phenotype, Double>()

        private val _cowPhenotypes = CompletableDeferred<List<CowGenotype.Phenotype>>()
        val cowPhenotypes: Deferred<List<CowGenotype.Phenotype>> get() = _cowPhenotypes

        fun randomTileCoordinate() = with(odorWorld.tileMap) { random.nextGridCoordinate() }
        private val lakeSize
            get() = random.nextInt(2,8)

        val odorWorld = OdorWorldComponent("Odor World 1").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            loadTileMap("empty.tmx")
            with(tileMap) {
                updateMapSize(32, 32)
                fill("Grass1")
            }
        }
        val lakeLayer = odorWorld.tileMap.run{
            addLayer(createTileMapLayer("Lake Layer"))
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
                decayFunction.dispersion = EntityType.COW.imageWidth / 1.4
            }.also { entity.addSensor(it) }
        }
        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }


        init {
            List(1) { randomTileCoordinate() }.forEach {
                with(odorWorld.tileMap) {
                    makeLake(it, lakeSize, lakeSize, lakeLayer)
                }
            }
            workspace.launch {
                (cowPhenotypes.await() zip entities).forEach { (phenotype, entity) ->
                    addUpdateActions(phenotype, entity)
                }
            }
        }

        fun addUpdateActions(cow: CowGenotype.Phenotype, entity: OdorWorldEntity) {

            val thirstNeuron = cow.drives.neuronList.first()

            fun addFitness(fitnessDelta: Double) {
                cowFitnesses[cow] = (cowFitnesses[cow]?:0.0) + fitnessDelta
            }

            // What to do when a cow finds water
            workspace.addUpdateAction("water found") {
                with(odorWorld.tileMap) {
                    centerLakeSensors[entity]?.let { sensor ->
                        // Water found
                        if (sensor.currentValue > 0.5) {
                            // Reset thirst node
                            thirstNeuron.forceSetActivation(0.0)
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
                thirstNeuron.forceSetActivation(thirstNeuron.activation + 0.005)
                addFitness(-thirstNeuron.activation)
                // if (thirstNeuron.activation > thirstThreshold) {
                //     // Thirsty! Reduce fitness
                //     // addFitness(-(thirstNeuron.activation - thirstThreshold) * (20.0 / iterationsPerRun))
                // } else {
                //     // Satiated. Increase fitness. Scale by iterations by run so that 10 is max fitness from
                //     // satiation per trial.
                //     addFitness(10.0 / iterationsPerRun)
                // }
            }

            // Impose a fitness cost for motion and increase thirst with motion
            workspace.addUpdateAction("update energy") {
                // val outputsActivations =
                //     cow.outputs.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                // val allActivations =
                //     (cow.inputs.neuronList + cow.outputs.neuronList).activations.sumOf { abs(it) } * 2
                // val energy = (outputsActivations + allActivations) * (1 / iterationsPerRun)
                val energy = (entity.speed * entity.speed)  / (iterationsPerRun*2)
                // addFitness(-energy)
                // thirstNeuron.activation += energy
            }
        }

        override suspend fun build() {
            if (!_cowPhenotypes.isCompleted) {
                // Express the genotypes
                _cowPhenotypes.complete(cowGenotypes.zip(networks).map { (genotype, network) -> genotype.expressWith(network) })
                // Make couplings
                with(workspace.couplingManager) {
                    val cows = _cowPhenotypes.await()
                    (0..cows.lastIndex).map { i ->
                        val cow = cows[i]
                        val sensor = sensors[i]
                        val effector = effectors[i]
                        sensor couple cow.inputs.neuronList
                        cow.outputs.neuronList couple effector
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
            return cowFitnesses.values.min()
        }
    }

    workspace.launch {
        val progressWindow = ProgressWindow(maxGenerations, "Fitness").apply {
            minimumSize = Dimension(300, 100)
            setLocationRelativeTo(null)
        }
        val cowSims = evaluator2(
            populatingFunction = { CowSim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            stoppingFunction = {
                nthPercentileFitness(10) > -1000 || generation > maxGenerations
            },
            peek = {
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${nthPercentileFitness(it).format(3)}"
                }.also {
                    println("[$generation] $it")
                    progressWindow.text = "5th Percentile Fitness: ${nthPercentileFitness(10).format(3)}"
                    progressWindow.value = generation
                }
            }
        )
        cowSims.take(1).forEach {
            with(it.visualize(workspace) as CowSim) {
                build()
                cowPhenotypes.await().forEach {
                    it.inputs.location = point( 0, 150)
                    it.hiddens.location = point( 0, 60)
                    it.outputs.location = point(0, -25)
                    it.drives.location = point(200, 60)
                }
            }
        }
        progressWindow.close()
    }
}