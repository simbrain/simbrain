package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.CompletableDeferred
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.activations
import org.simbrain.network.groups.NeuronCollection
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
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
    val iterationsPerRun = 1000

    class EvolvePursuerGenotype(seed: Long = Random.nextLong()) : Genotype {

        override val random: Random = Random(seed)

        var inputChromosome = chromosome(3) {
            add(nodeGene { isClamped = true })
        }
        var driveChromosome = chromosome(1) {
            add(nodeGene { isClamped = true; upperBound = 100.0; lowerBound = 0.0; label = "Thirst" })
            add(nodeGene { isClamped = true; upperBound = 100.0; lowerBound = 0.0 })
        }
        var hiddenChromosome = chromosome(2) { add(nodeGene()) }
        var outputChromosome = chromosome(3) { add(nodeGene { upperBound = 10.0; lowerBound = -10.0 }) }
        var connectionChromosome = chromosome(1) {
            repeat(3) {
                add(connectionGene(inputChromosome.sampleOne(), hiddenChromosome.sampleOne()))
                add(connectionGene(hiddenChromosome.sampleOne(), outputChromosome.sampleOne()))
            }
            add(connectionGene(driveChromosome.first(), hiddenChromosome.sampleOne()))
        }
        var layoutChromosome = chromosome(1) { 
            add(layoutGene()) 
        }

        inner class Phenotype(
            val driveNeurons: NeuronCollection,
            val inputNeurons: NeuronCollection,
            val hiddenNeurons: NeuronCollection,
            val outputNeurons: NeuronCollection,
            val connections: List<Synapse>,
            val layout: Layout
        )

        suspend fun expressWith(network: Network): Phenotype {
            val driveNeurons = NeuronCollection(network, network.express(driveChromosome)).also {
                network.addNetworkModelAsync(it); it.label = "drives"
            }
            val inputNeurons = NeuronCollection(network, network.express(inputChromosome)).also {
                network.addNetworkModelAsync(it); it.label = "inputs"
            }
            val hiddenNeurons = NeuronCollection(network, network.express(hiddenChromosome)).also {
                network.addNetworkModelAsync(it); it.label = "hidden"
            }
            val outputNeurons = NeuronCollection(network, network.express(outputChromosome)).also {
                network.addNetworkModelAsync(it); it.label = "outputs"
            }
            val connections = network.express(connectionChromosome)
            val layout = express(layoutChromosome).first().express()
            layout.layoutNeurons(hiddenNeurons.neuronList)
            return Phenotype(driveNeurons, inputNeurons, hiddenNeurons, outputNeurons, connections, layout)
        }

        fun copy() = EvolvePursuerGenotype(random.nextLong()).apply {
            val current = this@EvolvePursuerGenotype
            val new = this@apply

            new.driveChromosome = current.driveChromosome.copy()
            new.inputChromosome = current.inputChromosome.copy()
            new.hiddenChromosome = current.hiddenChromosome.copy()
            new.outputChromosome = current.outputChromosome.copy()
            new.connectionChromosome = current.connectionChromosome.copy()
            new.layoutChromosome = current.layoutChromosome.copy()
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

            // Ensure existing connections are not used when creating new connections
            val existingConnections = connectionChromosome.map { it.source to it.target }.toSet()
            val availableConnections = ((inputChromosome + hiddenChromosome + driveChromosome) cartesianProduct hiddenChromosome) +
                    (hiddenChromosome cartesianProduct outputChromosome) - existingConnections
            if (random.nextDouble() < 0.25 && availableConnections.isNotEmpty()) {
                val (source, target) = availableConnections.sampleOne()
                connectionChromosome.add(connectionGene(source, target) { strength = random.nextDouble(-1.0, 1.0) })
            }

            // Add a new hidden unit
            if (random.nextDouble() < 0.8) {
                hiddenChromosome.add(nodeGene())
            }

            layoutChromosome.forEach {
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

        var calories = 400.0

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

        init {
            List(1) { randomTileCoordinate() }.forEach {
                with(odorWorld.tileMap) {
                    makeLake(it, lakeSize, lakeSize, lakeLayer)
                }
            }

            evolvedAgent.addDefaultEffectors()
            evolvedAgent.addSensor(centerLakeSensor)

            workspace.addUpdateAction("update energy") {
                with(phenotypeDeferred.await()) {
                    val outputsActivations = outputNeurons.activations.sumOf { 1.2.pow(if (it < 0) it * -2 else it) - 1 }
                    val allActivations = (inputNeurons.neuronList + hiddenNeurons.neuronList).activations.sumOf { abs(it) } * 2
                    calories = max(0.0, calories - (outputsActivations + allActivations) * (1 / iterationsPerRun))
                    val (thirstNeuron, secondNeuron) = driveNeurons.neuronList
                    thirstNeuron.forceSetActivation(10.0 / iterationsPerRun + thirstNeuron.activation)
                }
            }

            // What to do when a cow finds water
            workspace.addUpdateAction("water found") {
                val (thirstNeuron, secondNeuron) = phenotypeDeferred.await().driveNeurons.neuronList
                with(odorWorld.tileMap) {
                    centerLakeSensor.let { sensor ->
                        // Water found
                        if (sensor.currentValue > 0.5) {
                            // Reset thirst
                            thirstNeuron.forceSetActivation(0.0)
                            // Drink the sugar water
                            calories += 100.0
                            // Relocate the lake
                            clear(lakeLayer)
                            val newLocation = randomTileCoordinate()
                            makeLake(newLocation, lakeSize, lakeSize, lakeLayer)
                        }
                    }
                }

            }
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
            
            // Build the sim then wait.
            build()
            
            val phenotype = phenotypeDeferred.await()
            
            // Iterate the sim
            workspace.iterateSuspend(iterationsPerRun)

            return calories - phenotype.driveNeurons.neuronList.first().activation * 4
        }

    }

    val progressWindow = ProgressWindow(maxGenerations, "Error").apply {
        minimumSize = Dimension(300, 100)
        setLocationRelativeTo(null)
    }
    val lastGeneration = evaluator(
        populatingFunction = { EvolveResourcePursuerSim() },
        populationSize = 100,
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
            nthPercentileFitness(5) > 1000.0 || generation > maxGenerations
        }
    )

    lastGeneration.take(1).forEach {
        with(it.visualize(workspace) as EvolveResourcePursuerSim) {
            build()
            val phenotype = this.phenotypeDeferred.await()

            phenotype.apply {
                driveNeurons.location = point(-150, 150)
                inputNeurons.location = point(0, 150)
                hiddenNeurons.location = point(0, 60)
                outputNeurons.location = point(0, -25)
            }

        }
    }
    progressWindow.close()
}
