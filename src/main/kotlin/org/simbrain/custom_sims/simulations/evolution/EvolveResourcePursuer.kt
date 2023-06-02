package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.activations
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.labels
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.updaterules.interfaces.BiasedUpdateRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.util.*
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.piccolo.GridCoordinate
import org.simbrain.util.piccolo.toPixelCoordinate
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

val evolveResourcePursuer = newSim {

    val scope = MainScope()

    /**
     * Max generation to run before giving up
     */
    val maxGenerations = 50

    /**
     * Iterations to run for each simulation. If < 3000 success is usually by luck.
     */
    val iterationsPerRun = 4000

    val thirstThreshold = 5.0

    fun createEvolution(): Evaluator {
        val evolutionarySimulation = evolutionarySimulation(1) {

            val inputs = chromosome(3) {
                nodeGene()
            }

            val metrics = chromosome(
                nodeGene {
                    label = "Thirst"
                    isClamped = true
                    forceSetActivation(0.5)
                }
            )

            val hiddens = chromosome(8) {
                nodeGene {
                    updateRule = DecayRule().apply {
                        decayFraction = .01
                    }
                }
            }

            val outputs = chromosome(3) { index ->
                nodeGene {
                    updateRule.let {
                        if (it is BoundedUpdateRule) {
                            it.lowerBound = -10.0
                            it.upperBound = 10.0
                        }
                        if (it is BiasedUpdateRule) {
                            it.bias = 1.0
                        }
                    }
                }
            }

            // Pre-populate with a few connections
            val connections = chromosome(
                connectionGene(inputs[0], hiddens[0]),
                connectionGene(inputs[1], hiddens[1]),
                connectionGene(inputs[1], hiddens[0]),
                connectionGene(inputs[1], hiddens[1]),
                connectionGene(hiddens[1], outputs[0]),
                connectionGene(hiddens[1], outputs[1]),
                connectionGene(hiddens[2], outputs[0]),
                connectionGene(hiddens[2], outputs[1])
            )

            val evolutionWorkspace = EvolutionWorkspace()

            val networkComponent = evolutionWorkspace { addNetworkComponent("Avoider") }

            val network = networkComponent.network

            // TODO: A way to consolidate the code a bit?

            val odorworldComponent = evolutionWorkspace {
                addOdorWorldComponent("World")
            }

            // TODO
            // withGui {
            //     place(odorworldComponent) {
            //         location = point(410, 0)
            //         width = 400
            //         height = 400
            //     }
            // }

            val odorworld = odorworldComponent.world.apply {
                isObjectsBlockMovement = true
                wrapAround = false
                // tileMap.updateMapSize(40, 40);
                // Grass = 5+1, Water = 0+1, Berries = 537+1
                // tileMap.editTile(10, 10, 6)
            }

            val sensors = chromosome(3) {
                tileSensorGene {
                    tileType = "water"
                    theta = it * 2 * 60.0
                    radius = 64.0
                    decayFunction.dispersion = 200.0
                    showDispersion = true
                }
            }

            val straightMovement = chromosome(1) {
                straightMovementGene()
            }

            val turning = chromosome(
                turningGene { direction = -1.0 },
                turningGene { direction = 1.0 }
            )

            val mouse = odorworld.addEntity(EntityType.MOUSE).apply {
                location = point(200.0, 200.0)
            }

            fun OdorWorldEntity.reset() {
                location = point(random.nextDouble() * 300, random.nextDouble() * 300)
            }

            // Take the current chromosomes,and express them via an agent in a world.
            // Everything needed to build one generation
            // Called once for each genome at each generation
            onBuild { visible ->
                network {
                    if (visible) {
                        val inputGroup = +inputs.asNeuronCollection {
                            label = "Input"
                            layout(LineLayout())
                            location = point(250, 280)
                        }
                        inputGroup.neuronList.labels = listOf("center", "left", "right")
                        +metrics
                        +hiddens.asNeuronCollection {
                            label = "Hidden"
                            layout(GridLayout())
                            location = point(0, 100)
                        }
                        val outputGroup = +outputs.asNeuronCollection {
                            label = "Output"
                            layout(LineLayout())
                            location = point(250, 40)
                            setNeuronType(outputs[0].template.updateRule)
                        }
                        outputGroup.neuronList.labels = listOf("straight", "left", "right")
                    } else {
                        // This is update when graphics are off
                        +inputs
                        +metrics
                        +hiddens
                        +outputs
                    }
                    +connections
                }
                mouse {
                    +sensors
                    +straightMovement
                    +turning
                }

                evolutionWorkspace {
                    runBlocking {
                        couplingManager.apply {
                            val (straightNeuron, leftNeuron, rightNeuron) = outputs.getProducts()
                            val (straightConsumer) = straightMovement.getProducts()
                            val (left, right) = turning.getProducts()

                            sensors.getProducts() couple inputs.getProducts()
                            straightNeuron couple straightConsumer
                            leftNeuron couple left
                            rightNeuron couple right
                        }
                    }
                }
            }

            //
            // Mutate the chromosomes. Specify what things are mutated at each generation.
            //
            onMutate {
                hiddens.forEach {
                    it.mutate {
                        updateRule.let {
                            if (it is BiasedUpdateRule) it.bias += random.nextDouble(-0.2, 0.2)
                        }
                    }
                }
                if (Random.nextDouble() > 0.5) {
                    hiddens.add(nodeGene())
                }

                connections.forEach {
                    it.mutate {
                        strength += random.nextDouble(-0.5, 0.5)
                    }
                }

                if (Random.nextDouble() > 0.5) {
                    // Random source neuron
                    val source = (inputs + hiddens + metrics).selectRandom()
                    // Random target neuron
                    val target = (outputs + hiddens).selectRandom()
                    // Add the connection
                    connections += connectionGene(source, target) {
                        strength = random.nextDouble(-10.0, 10.0)
                    }
                }

            }

            fun Workspace.onReachingWater(thirstNeuron: Neuron, world: OdorWorld, entity: OdorWorldEntity, good: () -> Unit, bad: (thirst: Double) -> Unit) {

                fun randomTileCoordinate() = GridCoordinate(
                    random.nextInt(world.tileMap.width).toDouble(),
                    random.nextInt(world.tileMap.height).toDouble()
                )

                fun setTile(coordinate: GridCoordinate, tileId: Int) {
                    val (x, y) = coordinate
                    world.tileMap.setTile(x.toInt(), y.toInt(), tileId)
                }

                val currentWaterLocations = List(3) { randomTileCoordinate() }.toMutableSet().onEach {
                    setTile(it, 3)
                }

                addUpdateAction("location check") {
                    with(world.tileMap) {
                        currentWaterLocations.toList().forEach { currentWaterLocation ->
                            val distance = currentWaterLocation.toPixelCoordinate().distanceTo(entity.location)
                            if (distance < entity.width / 2) {
                                good()
                                thirstNeuron.forceSetActivation(0.0)

                                setTile(currentWaterLocation, 0)
                                currentWaterLocations.remove(currentWaterLocation)

                                val newLocation = randomTileCoordinate()
                                currentWaterLocations.add(newLocation)
                                setTile(newLocation, 3)
                            }
                        }
                    }
                }

                addUpdateAction("update thirst") {
                    thirstNeuron.forceSetActivation(thirstNeuron.activation + 0.005)
                    if (thirstNeuron.activation > thirstThreshold) {
                        bad(thirstNeuron.activation)
                    }
                }
            }

            //
            // Evaluate the current generation.
            //
            onEval {
                var fitness = 0.0

                evolutionWorkspace.onReachingWater(metrics.getProducts().first(), odorworld, mouse, {
                    // fitness += 1
                }) {
                    fitness -= (it - thirstThreshold) * (20.0 / iterationsPerRun)
                }

                evolutionWorkspace.addUpdateAction("update thirst fitness") {
                    val (thirst) = metrics.getProducts()
                    if (thirst.activation < thirstThreshold) {
                        fitness += (10.0 / iterationsPerRun)
                    }
                }

                evolutionWorkspace.addUpdateAction("update energy") {
                    val (thirst) = metrics.getProducts()
                    val outputsActivations = outputs.getProducts().activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                    val allActivations = (inputs + hiddens).getProducts().activations.sumOf { abs(it) } * 2
                    thirst.activation += (outputsActivations + allActivations) * (1 / iterationsPerRun)
                }

                evolutionWorkspace.apply {
                    iterateSuspend(iterationsPerRun)
                }

                fitness
            }

            // Called when evolution finishes. evolutionWorkspace is the "winning" sim.
            onPeek {
                workspace.openFromZipData(evolutionWorkspace.zipData)
                val worldComponent = workspace.componentList.filterIsInstance<OdorWorldComponent>().first()
                val world = worldComponent.world
                val newMouse = world.entityList.first()
                val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
                val network = networkComponent.network
                val thirstNeuron = network.getModelByLabel<Neuron>("Thirst")
                workspace.onReachingWater(thirstNeuron, world, newMouse, { println("hit") }) {
                    println("bad")
                }
            }
        }

        return evaluator(evolutionarySimulation) {
            populationSize = 100
            eliminationRatio = 0.5
            optimizationMethod = Evaluator.OptimizationMethod.MAXIMIZE_FITNESS
            runUntil { generation == maxGenerations || fitness > 12 }
        }
    }

    scope.launch {
        workspace.clearWorkspace()

        val progressWindow = if (desktop != null) {
            ProgressWindow(maxGenerations, "Fitness")
        } else {
            null
        }

        launch(Dispatchers.Default) {

            val generations = createEvolution().start().onEachGenerationBest { agent, gen ->
                if (progressWindow == null) {
                    println("[$gen] Fitness: ${agent.fitness.format(2)}")
                } else {
                    progressWindow.value = gen
                    progressWindow.text = "Fitness: ${agent.fitness.format(2)}"
                }
            }

            val (best, _) = generations.best

            // println(best)

            val build = best.visibleBuild()

            build.peek()

            progressWindow?.close()

        }
    }

}

fun main() {
    evolveResourcePursuer.run()
}