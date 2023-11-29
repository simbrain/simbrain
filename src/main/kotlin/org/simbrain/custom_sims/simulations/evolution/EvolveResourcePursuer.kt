package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.bound
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.cartesianProduct
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.abs
import kotlin.random.Random

/**
 * Initial work with Luis getting back to evo algorithms.
 *
 * Cangelosi and a better energy model, including cost of having neuron. A real physical model of kcals involved in
 * moving, supporting each neuron (or maybe even every spike). A model of eating and digestion. You get food, and it
 * provides calories but decays as its digested
 *
 * Goal: Compare the model with various levels of energy constraint and see the differences
 */
val evolveResourcePursuer = newSim {

    /**
     * Max generation to run before giving up
     */
    val maxGenerations = 15

    /**
     * Iterations to run for each simulation. If < 3000 success is usually by luck.
     */
    val iterationsPerRun = 100

    val thirstThreshold = 5.0

    class EvolvePursuerGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        // TODO (Work on some of this is in evolveresourcepursuer2.kt)
        // Fixed input nodes with couplings (not evolved)
        // Hidden Nodes
        // Output nodes with couplings (not evolved)
        // Drive / Fixed nodes

        // Improve energy model and fold into fitness function

        // Connection Strategy
        // Layout Strategy

        var motivationNodeChromosome = chromosome(2) {
            add(nodeGene {
                label = "Fixed node ${it + 1}"
                location = point(it * 100, -50)
                lowerBound = -10.0
                upperBound = 10.0
            })
        }
        var nodeChromosome = chromosome(2) {
            add(nodeGene { upperBound = 10.0; lowerBound = -10.0 })
        }
        // TODO: Evolve a connection strategy
        var connectionChromosome = chromosome<Synapse, ConnectionGene>()

        var layoutChromosome = chromosome(1) { 
            add(layoutGene()) 
        }

        inner class Phenotype(
            val layout: Layout,
            val motivations: List<Neuron>,
            val nodes: List<Neuron>,
            val connections: List<Synapse>
        )

        suspend fun expressWith(network: Network): Phenotype {
            val layout = express(layoutChromosome).first().express()
            val motivations = network.express(motivationNodeChromosome)
            val nodes = network.express(nodeChromosome)
            val connections = network.express(connectionChromosome)
            layout.layoutNeurons(nodes)
            return Phenotype(layout, motivations, nodes, connections)
        }

        fun copy() = EvolvePursuerGenotype(random.nextLong()).apply {
            val current = this@EvolvePursuerGenotype
            val new = this@apply

            new.layoutChromosome = current.layoutChromosome.copy()
            new.motivationNodeChromosome = current.motivationNodeChromosome.copy()
            new.nodeChromosome = current.nodeChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
        }

        fun mutate() {

            nodeChromosome.forEach {
                it.mutate {
                    with(dataHolder as BiasedScalarData) {
                        bias += random.nextDouble(-1.0, 1.0)
                    }
                }
            }

            motivationNodeChromosome.forEach {
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

            // Ensure existing connections are not used when creating new connections
            val existingConnections = connectionChromosome.map { it.source to it.target }.toSet()
            val availableConnections = (nodeChromosome cartesianProduct nodeChromosome) - existingConnections
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne()
                connectionChromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.1) {
                nodeChromosome.add(nodeGene())
            }

            layoutChromosome.forEach {
                fun LayoutGene.mutateParam() = mutate {
                    hSpacing += random.nextDouble(-1.0, 1.0)
                    vSpacing += random.nextDouble(-1.0, 1.0)
                }
                fun LayoutGene.mutateType() = mutate {
                    when (random.nextDouble()) {
                        in 0.0..0.5 -> layoutType = GridLayout()
                        in 0.5..1.0 -> layoutType = HexagonalGridLayout()
                        // in 0.1..0.15 -> layout = LineLayout()
                    }
                }
                it.mutateParam()
                it.mutateType()
            }

        }

    }

    class EvolveResourcePursuerSim(
        val evolvePursuerGenotype: EvolvePursuerGenotype = EvolvePursuerGenotype(),
        val workspace: Workspace = Workspace()
    ) : EvoSim {

        val random = Random(42)

        val networkComponent = NetworkComponent("Network").also { workspace.addWorkspaceComponent(it) }

        val network = networkComponent.network

        val phenotypeDeferred = CompletableDeferred<EvolvePursuerGenotype.Phenotype>()

        val thirstThreshold = 5.0
        val fitness = 0.0

        fun randomTileCoordinate() = with(odorWorld.tileMap) { random.nextGridCoordinate() }
        private val lakeSize
            get() = random.nextInt(2, 8)

        val odorWorld = OdorWorldComponent("Odor World").also {
            workspace.addWorkspaceComponent(it)
        }.world.apply {
            loadTileMap("empty.tmx")
            with(tileMap) {
                updateMapSize(32, 32)
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
        }.also { evolvedAgent.addSensor(it) }

        var numDrinks = 0

        init {
            List(1) { randomTileCoordinate() }.forEach {
                with(odorWorld.tileMap) {
                    makeLake(it, lakeSize, lakeSize, lakeLayer)
                }
            }

            evolvedAgent.addDefaultEffectors()
            evolvedAgent.addSensor(centerLakeSensor)

            // val thirstNeuron = phenotype.neuronList.first()

            // What to do when a cow finds water
            workspace.addUpdateAction("water found") {
                with(odorWorld.tileMap) {
                    centerLakeSensor.let { sensor ->
                        // Water found
                        if (sensor.currentValue > 0.5) {

                            // Reset thirst node
                            // TDDO

                            // thirstNeuron.forceSetActivation(0.0)
                            //     fun addFitness(fitnessDelta: Double) {
                            //         cowFitnesses[cow] = (cowFitnesses[cow]?:0.0) + fitnessDelta
                            //     }
                            
                            // Update fitness
                            numDrinks++
                            // Relocate the lake
                            clear(lakeLayer)
                            val newLocation = randomTileCoordinate()
                            makeLake(newLocation, lakeSize, lakeSize, lakeLayer)
                        }
                    }
                }

            }


            // Update thirst and fitness
            // workspace.addUpdateAction("update thirst") {
            //     thirstNeuron.forceSetActivation(thirstNeuron.activation + 0.005)
            //     addFitness(-thirstNeuron.activation)
            //     // if (thirstNeuron.activation > thirstThreshold) {
            //     //     // Thirsty! Reduce fitness
            //     //     // addFitness(-(thirstNeuron.activation - thirstThreshold) * (20.0 / iterationsPerRun))
            //     // } else {
            //     //     // Satiated. Increase fitness. Scale by iterations by run so that 10 is max fitness from
            //     //     // satiation per trial.
            //     //     addFitness(10.0 / iterationsPerRun)
            //     // }
            // }

            //     // Impose a fitness cost for motion and increase thirst with motion
            //     workspace.addUpdateAction("update energy") {
            //         // val outputsActivations =
            //         //     cow.outputs.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
            //         // val allActivations =
            //         //     (cow.inputs.neuronList + cow.outputs.neuronList).activations.sumOf { abs(it) } * 2
            //         // val energy = (outputsActivations + allActivations) * (1 / iterationsPerRun)
            //         val energy = (entity.speed * entity.speed)  / (iterationsPerRun*2)
            //         // addFitness(-energy)
            //         // thirstNeuron.activation += energy
            //     }
            // }
        }

        override fun mutate() {
            evolvePursuerGenotype.mutate()
        }

        override suspend fun build() {
            if (!phenotypeDeferred.isCompleted) {
                // Express the genotypes
                phenotypeDeferred.complete(evolvePursuerGenotype.expressWith(network))
                // Make couplings
                //  TODO: Improve this
                with(workspace.couplingManager) {
                    sensors couple network.freeNeurons
                    network.freeNeurons couple evolvedAgent.effectors
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
            
            // Build the sim then wait.
            build()
            
            //  TODO: Agent network?
            val phenotype = phenotypeDeferred.await()
            
            // Iterate the sim
            workspace.iterateSuspend(iterationsPerRun)

            // Comment / Uncomment different choices of fitness function here
            // Move some of this to update function or reverse
            suspend fun fitness(): Double {
                val avgLength = phenotype.connections.lengths.average()
                val numWeights = phenotype.connections.size
                val avgActivation = phenotype.nodes.activations.average()
                val totalActivation = phenotype.nodes.activations.sum()
                // Evolve fixed nodes to have specific activations 2.5 and -3
                val (m1, m2) = phenotype.motivations
                val m1error = abs(m1.activation - 2.5)
                val m2error = abs(m2.activation + 3)
                // TODO: Normalize errors and provide for weightings
                val numNodesError = abs(phenotype.nodes.size - 20).toDouble()
                val numWeightsError = abs(numWeights - 40)
                val axonLengthError = abs(avgLength - 250)
                val avgActivationError = abs(avgActivation - 5)
                val totalActivationError = abs(totalActivation - 10)
                // Area in thousands of pixels
                val bounds = network.freeNeurons.bound
                val size = (bounds.height * bounds.width) / 10_000
                val areaError = abs(size - 10)
                return -numDrinks + numNodesError + totalActivationError
            
            }
            // print("${network.looseNeurons.size},")
            // return -fitness()
            return numDrinks.toDouble()
        }

    }

    val progressWindow = ProgressWindow(maxGenerations, "Error").apply {
        minimumSize = Dimension(300, 100)
        setLocationRelativeTo(null)
    }
    val lastGeneration = evaluator(
        populatingFunction = { EvolveResourcePursuerSim() },
        populationSize = 1000,
        eliminationRatio = 0.25,
        peek = {
            listOf(0, 10, 25, 50, 75, 90, 100).joinToString(" ") {
                "$it: ${nthPercentileFitness(it).format(3)}"
            }.also {
                println("[$generation] $it")
                progressWindow.text = "5th Percentile MSE: ${nthPercentileFitness(5).format(3)}"
                progressWindow.value = generation
            }
        },
        stoppingFunction = {
            nthPercentileFitness(5) > 10.0 || generation > maxGenerations
        }
    )

    lastGeneration.take(1).forEach {
        with(it.visualize(workspace) as EvolveResourcePursuerSim) {
            build()
            val phenotype = this.phenotypeDeferred.await()
        }
    }
    progressWindow.close()
}
