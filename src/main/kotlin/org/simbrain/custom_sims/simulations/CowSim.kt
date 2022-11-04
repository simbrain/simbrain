package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.*
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithm2.*
import org.simbrain.util.piccolo.*
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.TileSensor
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

val evolveCow = newSim {

    class CowGenotype(seed: Long = Random.nextLong()) : Genotype2 {
        override val random: Random = Random(seed)
        var inputs = chromosome2(3) { add(nodeGene2 { isClamped = true }) }
        var hiddens = chromosome2(2) { add(nodeGene2()) }
        var outputs = chromosome2(3) { add(nodeGene2 { upperBound = 10.0; lowerBound = -10.0 }) }
        var drives = chromosome2(1) { add(nodeGene2 { isClamped = true }) }
        var connections = chromosome2(0) { add(connectionGene2(inputs.first(), outputs.first())) }


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


            if (random.nextDouble() < 0.9) {
                val source = (inputs + hiddens + outputs + drives).toList().sampleWithoutReplacement().first()
                val target = (hiddens + outputs).toList().sampleWithoutReplacement().first()
                connections.add(connectionGene2(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

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

        val odorWorld = OdorWorldComponent("Odor World 1").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            loadTileMap("empty.tmx")
            tileMap.updateMapSize(32, 32)
            tileMap.fill(6)
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
                    showDispersion = true
                }.also { entity.addSensor(it) }
            }
        }

        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }

        private val _phenotypes = CompletableDeferred<List<CowGenotype.Phenotype>>()

        val phenotypes: Deferred<List<CowGenotype.Phenotype>> get() = _phenotypes

        val thirstThreshold = 5.0

        val iterationsPerRun = 4000

        val waterLocations = HashSet<GridCoordinate>()

        fun randomTileCoordinate() = GridCoordinate(
            random.nextInt(odorWorld.tileMap.width).toDouble(),
            random.nextInt(odorWorld.tileMap.height).toDouble()
        )

        fun setTile(coordinate: GridCoordinate, tileId: Int) {
            val lakeSize = 2
            with (odorWorld.tileMap) {
                if (tileId == 6) {
                    val (x, y) = coordinate
                    (x.toInt() until (x.toInt() + lakeSize)).map { i ->
                        (y.toInt() until (y.toInt() + lakeSize)).map { j ->
                            if (point(i, j).asGridCoordinate().isInMap) {
                                setTile(i, j, tileId)
                            }
                        }
                    }
                } else {
                    makeLake(coordinate, lakeSize, lakeSize)
                }
            }
        }

        init {
            List(1) { randomTileCoordinate() }.forEach {
                waterLocations.add(it)
                setTile(it, 3)
            }
            workspace.coroutineScope.launch {
                (phenotypes.await() zip entities).forEach { (phenotype, entity) ->
                    addUpdateActions(phenotype, entity)
                }
            }
        }

        fun addUpdateActions(phenotype: CowGenotype.Phenotype, entity: OdorWorldEntity) {

            val thirstNeuron = phenotype.drives.neuronList.first()

            workspace.addUpdateAction("location check") {
                with(odorWorld.tileMap) {
                    waterLocations.toList().forEach { currentWaterLocation ->
                        val distance = currentWaterLocation.toPixelCoordinate().distanceTo(entity.location)
                        if (distance < entity.width / 2) {
                            fitness += 1.0 / iterationsPerRun
                            thirstNeuron.forceSetActivation(0.0)

                            setTile(currentWaterLocation, 6)
                            waterLocations.remove(currentWaterLocation)

                            val newLocation = randomTileCoordinate()
                            waterLocations.add(newLocation)
                            setTile(newLocation, 3)
                        }
                    }
                }
            }

            workspace.addUpdateAction("update thirst") {
                thirstNeuron.forceSetActivation(thirstNeuron.activation + 0.005)
                if (thirstNeuron.activation > thirstThreshold) {
                    fitness -= (thirstNeuron.activation - thirstThreshold) * (20.0 / iterationsPerRun)
                } else {
                    fitness += (10.0 / iterationsPerRun)
                }
            }


            workspace.addUpdateAction("update energy") {
                val outputsActivations =
                    phenotype.outputs.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                val allActivations =
                    (phenotype.inputs.neuronList + phenotype.outputs.neuronList).activations.sumOf { abs(it) } * 2
                thirstNeuron.activation += (outputsActivations + allActivations) * (1 / iterationsPerRun)
            }
        }

        var fitness = 0.0

        override suspend fun build() {
            if (_phenotypes.isActive) {
                _phenotypes.complete(cowGenotypes.zip(networks).map { (genotype, network) -> genotype.build(network) })
                with(workspace.couplingManager) {
                    val cows = _phenotypes.await()
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

            return fitness
        }


    }

    workspace.coroutineScope.launch {
        val cowSims = evaluator2(
            populatingFunction = { CowSim() },
            populationSize = 100,
            eliminationRatio = 0.5,
            stoppingFunction = {
                nthPercentileFitness(5) > 10 || generation > 50
            },
            peek = {
                listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                    "$it: ${nthPercentileFitness(it).format(3)}"
                }.also { println("[$generation] $it") }
            }
        )

        cowSims.take(1).forEach {
            val winningSim = it.visualize(workspace)
            winningSim.build()

        }
    }
}