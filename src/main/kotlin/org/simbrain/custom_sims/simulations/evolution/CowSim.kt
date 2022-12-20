package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
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
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

val maxGenerations = 50
val iterationsPerRun = 2000

val evolveCow = newSim {

    class CowGenotype(seed: Long = Random.nextLong()) : Genotype2 {
        override val random: Random = Random(seed)
        var inputs = chromosome2(3) { add(nodeGene2 { isClamped = true }) }
        var hiddens = chromosome2(2) { add(nodeGene2()) }
        var outputs = chromosome2(3) { add(nodeGene2 { upperBound = 10.0; lowerBound = -10.0 }) }
        var drives = chromosome2(1) { add(nodeGene2 { isClamped = true }) }
        var connections = chromosome2(1) {
            repeat(3) {
                add(connectionGene2(inputs.sampleOne(), hiddens.sampleOne()))
                add(connectionGene2(hiddens.sampleOne(), outputs.sampleOne()))
            }
            add(connectionGene2(drives[0], hiddens.sampleOne()))
        }

        inner class Phenotype(
            val inputs: NeuronCollection,
            val hiddens: NeuronCollection,
            val outputs: NeuronCollection,
            val drives: NeuronCollection,
            val connections: List<Synapse>
        )

        suspend fun build(network: Network): Phenotype {
            return Phenotype(
                NeuronCollection(network, network.express(inputs)).also {
                    network.addNetworkModel(it); it.label = "input"
                },
                NeuronCollection(network, network.express(hiddens)).also {
                    network.addNetworkModel(it); it.label = "hidden"
                },
                NeuronCollection(network, network.express(outputs)).also {
                    network.addNetworkModel(it); it.label = "output"
                },
                NeuronCollection(network, network.express(drives)).also {
                    network.addNetworkModel(it); it.label = "drives"
                },
                network.express(connections)
            )
        }

        fun copy() = CowGenotype(random.nextLong()).apply {
            val current = this@CowGenotype
            val new = this@apply
            new.inputs = current.inputs.copy()
            new.hiddens = current.hiddens.copy()
            new.outputs = current.outputs.copy()
            new.drives = current.drives.copy()
            new.connections = current.connections.copy()
        }

        fun mutate() {
            hiddens.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }
            connections.forEach {
                it.mutate {
                    strength += random.nextDouble(-1.0, 1.0)
                }
            }

            val existingPairs = connections.map { it.source to it.target }.toSet()
            val availableConnections = ((inputs + hiddens + outputs) cartesianProduct (hiddens + outputs)) - existingPairs
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne(random)
                connections.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            val availablePairs = (drives cartesianProduct (inputs + hiddens + outputs)) - existingPairs
            if (random.nextDouble() < 0.25 && availablePairs.isNotEmpty()) {
                val (source, target) = availablePairs.sampleOne(random)
                connections.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Make hidden layer larger
            if (random.nextDouble() < 0.1) {
                hiddens.add(nodeGene2())
            }
        }
    }

    class CowSim(
        val cowGenotypes: List<CowGenotype> = List(2) { CowGenotype() },
        val workspace: Workspace = HeadlessWorkspace()
    ) : EvoSim {

        val random = Random(cowGenotypes.first().random.nextInt())

        val thirstThreshold = 5.0
        val fitness = mutableMapOf<CowGenotype.Phenotype, Double>()

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
        val lakeLayer = with(odorWorld.tileMap) {
            val layer = createTileMapLayer("Lake Layer")
            layers.add(layer)
            layer
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
        val sensors = entities.map { entity ->
            List(3) { index ->
                TileSensor("water", radius = 60.0, angle = (index * 120.0)).apply {
                    decayFunction.dispersion = 250.0
                }.also { entity.addSensor(it) }
            }
        }
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
            workspace.coroutineScope.launch {
                (cowPhenotypes.await() zip entities).forEach { (phenotype, entity) ->
                    addUpdateActions(phenotype, entity)
                }
            }
        }

        fun addUpdateActions(phenotype: CowGenotype.Phenotype, entity: OdorWorldEntity) {

            val thirstNeuron = phenotype.drives.neuronList.first()

            fun addFitness(fitnessDelta: Double) {
                fitness[phenotype] = (fitness[phenotype]?:0.0) + fitnessDelta
            }

            // What to do when a cow finds water
            workspace.addUpdateAction("water found") {
                with(odorWorld.tileMap) {
                    centerLakeSensors[entity]?.let { sensor ->
                        if (sensor.currentValue > 0.5) {
                            addFitness(1.0 / iterationsPerRun)
                            thirstNeuron.forceSetActivation(0.0)
                            clear( lakeLayer)
                            val newLocation = randomTileCoordinate()
                            makeLake(newLocation, lakeSize, lakeSize, lakeLayer)
                        }
                    }
                }
            }

            // Every iteration water is not found increase the thirst Neuron
            // Also update fitness on this basis
            workspace.addUpdateAction("update thirst") {
                thirstNeuron.forceSetActivation(thirstNeuron.activation + 0.005)
                if (thirstNeuron.activation > thirstThreshold) {
                    addFitness(-(thirstNeuron.activation - thirstThreshold) * (20.0 / iterationsPerRun))
                } else {
                    addFitness(10.0 / iterationsPerRun)
                }
            }


            // Increase thirst when more energy is used
            workspace.addUpdateAction("update energy") {
                val outputsActivations =
                    phenotype.outputs.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                val allActivations =
                    (phenotype.inputs.neuronList + phenotype.outputs.neuronList).activations.sumOf { abs(it) } * 2
                val energy = (outputsActivations + allActivations) * (1 / iterationsPerRun)
                addFitness(-energy)
                thirstNeuron.activation += energy
            }
        }

        override suspend fun build() {
            if (_cowPhenotypes.isActive) {
                _cowPhenotypes.complete(cowGenotypes.zip(networks).map { (genotype, network) -> genotype.build(network) })
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

        override fun copy() = CowSim(cowGenotypes.map { it.copy() }, HeadlessWorkspace())

        override suspend fun eval(): Double {
            build()
            workspace.iterateSuspend(iterationsPerRun)
            return fitness.values.min()
        }
    }

    workspace.coroutineScope.launch {
        val progressWindow = ProgressWindow(maxGenerations, "Fitness")
        val cowSims = evaluator2(
            populatingFunction = { CowSim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            stoppingFunction = {
                nthPercentileFitness(10) > 10 || generation > maxGenerations
            },
            peek = {
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${nthPercentileFitness(it).format(3)}"
                }.also {
                    println("[$generation] $it")
                    progressWindow.text = "Error: ${nthPercentileFitness(0).format(3)}"
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