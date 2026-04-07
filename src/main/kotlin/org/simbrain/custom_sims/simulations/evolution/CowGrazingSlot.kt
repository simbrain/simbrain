package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.sampleWithReplacement
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getRandomLocation
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.Dimension
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

// ======================================================================
// Generic Evaluator (Prototype)
//
// Works with any type T — no EvoSim interface needed.
// The caller provides create/copy/mutate as plain functions
// and evaluate as a suspend function. This means ALL setup
// (world creation, entity addition, etc.) can happen in a
// suspend context — no runBlocking needed.
// ======================================================================

suspend fun <T> genericEvaluator(
    populationSize: Int,
    eliminationRatio: Double,
    create: () -> T,
    copy: (T) -> T,
    mutate: (T) -> Unit,
    evaluate: suspend (T) -> Double,
    stoppingFunction: GenerationFitnessPair.() -> Boolean,
    peek: GenerationFitnessPair.() -> Unit = {},
    sortDescending: Boolean = true,
    seed: Long = Random.nextLong(),
    random: Random = Random(seed)
): List<T> = coroutineScope {
    var generation = 0
    var population = List(populationSize) { create() }
    do {
        generation++
        val scores = population.map { individual -> async { evaluate(individual) } }.awaitAll()
        val ranked = (population zip scores).shuffled(random).let {
            if (sortDescending) it.sortedByDescending { it.second }
            else it.sortedBy { it.second }
        }
        val eliminationCount = (ranked.size * eliminationRatio).roundToInt()
        val survivors = ranked.take(populationSize - eliminationCount).map { it.first }
        population = survivors.map { copy(it) } +
            survivors.sampleWithReplacement(random).take(eliminationCount).toList().map {
                copy(it).also { c -> mutate(c) }
            }
        val pair = GenerationFitnessPair(generation, ranked.map { it.second })
        peek(pair)
    } while (!stoppingFunction(pair))
    population
}

// Convenience for SlotGenotype (single genotype)
suspend fun <G : SlotGenotype> slotEvaluator(
    populationSize: Int,
    eliminationRatio: Double,
    genotypeFactory: () -> G,
    evaluate: suspend (G) -> Double,
    stoppingFunction: GenerationFitnessPair.() -> Boolean,
    peek: GenerationFitnessPair.() -> Unit = {},
    sortDescending: Boolean = true
): List<G> = genericEvaluator(
    populationSize = populationSize,
    eliminationRatio = eliminationRatio,
    create = genotypeFactory,
    copy = {
        @Suppress("UNCHECKED_CAST")
        it.copyGenotype() as G
    },
    mutate = { it.mutate() },
    evaluate = evaluate,
    stoppingFunction = stoppingFunction,
    peek = peek,
    sortDescending = sortDescending
)

// ======================================================================
// Cow Brain Genotype — using slot DSL (unchanged from v2)
// ======================================================================

class CowBrainGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

    val inputs by nodeChromosome(7) { clamped = true }
    val hidden by nodeChromosome(2)
    val outputs by nodeChromosome(3) { upperBound = 10.0; lowerBound = -10.0 }
    val connections by connectionChromosome()

    init {
        inputs.genes.last().template.activation = 1.0
        repeat(3) {
            connections.chromosome.add(connectionGene(inputs.genes.random(random), hidden.genes.random(random)))
            connections.chromosome.add(connectionGene(hidden.genes.random(random), outputs.genes.random(random)))
        }
        connections.chromosome.add(connectionGene(inputs.genes[3], hidden.genes.random(random)))
    }

    override fun createNew(seed: Long) = CowBrainGenotype(seed)

    override fun mutate() {
        hidden.genes.forEach { it.mutate { bias += random.nextDouble(-1.0, 1.0) } }
        connections.genes.forEach { it.mutate { strength += random.nextDouble(-1.0, 1.0) } }
        withProbability(0.25) {
            connections.addConnection(
                inputs to hidden, inputs to outputs,
                hidden to hidden, hidden to outputs,
                outputs to hidden, outputs to outputs
            ) { strength = random.nextDouble(-1.0, 1.0) }
        }
        if (random.nextDouble() < 0.1) {
            hidden.chromosome.add(nodeGene())
        }
    }
}

// ======================================================================
// Grazing Cows — no EvoSim, no CowSim class, no runBlocking
//
// The key insight: the evaluator works with genotypes directly.
// World setup happens inside a suspend `evaluate` function,
// so addEntity/tileMap.fill work naturally without runBlocking.
//
// A shared `buildCowWorld` function handles setup for both
// evaluation (headless workspace) and visualization (desktop workspace).
// ======================================================================

// Return type for the shared setup function
class CowWorld(
    val workspace: Workspace,
    val odorWorldComponent: OdorWorldComponent,
    val networkComponents: List<NetworkComponent>,
    val entities: List<OdorWorldEntity>,
    val dandelionSensors: List<List<ObjectSensor>>,
    val cowSensors: List<List<ObjectSensor>>,
    val effectors: List<List<org.simbrain.world.odorworld.effectors.Effector>>
)

val grazingCowsSlot = newSim { optionString ->

    var numCows = 2
    var maxGenerations = 50
    var iterationsPerRun = 2000
    var populationSize = 100
    var eliminationRatio = .5
    var numFlowers = 10
    var useAverage = false

    // Shared setup: creates world, entities, sensors, networks, expresses genotypes, wires couplings.
    // Suspend — no runBlocking needed.
    suspend fun buildCowWorld(
        genotypes: List<CowBrainGenotype>,
        workspace: Workspace
    ): CowWorld {
        // Odor world
        val odorWorldComponent = OdorWorldComponent("Odor World 1").also {
            workspace.addWorkspaceComponent(it)
        }
        val world = odorWorldComponent.world
        world.isObjectsBlockMovement = false
        world.tileMap.updateMapSize(25, 25)
        world.tileMap.fill("Grass1")

        // Networks
        val networkComponents = genotypes.indices.map { i ->
            NetworkComponent("Network ${i + 1}").also { workspace.addWorkspaceComponent(it) }
        }

        // Cow entities
        val entities = genotypes.indices.map { i ->
            OdorWorldEntity(world, EntityType.Cow).also {
                world.addEntity(it)
                it.location = point((i + 1) * 100, (i + 1) * 100)
            }
        }

        // Sensors
        val dandelionSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.Dandelions, radius = 60.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 250.0
                }.also { entity.addSensor(it) }
            }
        }
        val cowSensors = entities.map { entity ->
            List(3) { index ->
                ObjectSensor(EntityType.Cow, radius = 50.0, theta = (index * 120.0)).apply {
                    decayFunction.dispersion = 200.0
                }.also { entity.addSensor(it) }
            }
        }
        entities.forEach {
            it.addSensor(ObjectSensor(EntityType.Dandelions, radius = 0.0).apply {
                label = "centralFlowerSensor"
                decayFunction = StepDecayFunction()
                decayFunction.dispersion = 30.0
            })
        }

        // Effectors
        val effectors = entities.map { entity ->
            entity.addDefaultEffectors()
            entity.effectors
        }

        // Flowers
        repeat(numFlowers) {
            val loc = world.getRandomLocation()
            world.addEntity(loc.x.toInt(), loc.y.toInt(), EntityType.Dandelions, doubleArrayOf(1.0))
        }

        // Express genotypes into networks
        genotypes.zip(networkComponents).forEach { (genotype, nc) ->
            genotype.expressAll(nc.network)
        }

        // Couplings
        with(workspace.couplingManager) {
            genotypes.indices.forEach { i ->
                (dandelionSensors[i] + cowSensors[i]) couple genotypes[i].inputs.neurons.neuronList
                genotypes[i].outputs.neurons.neuronList couple effectors[i]
            }
        }

        return CowWorld(workspace, odorWorldComponent, networkComponents, entities, dandelionSensors, cowSensors, effectors)
    }

    // --- Evaluate one set of cow genotypes ---

    suspend fun evaluate(genotypes: List<CowBrainGenotype>): Double {
        val workspace = Workspace()
        val cowWorld = buildCowWorld(genotypes, workspace)

        // Fitness tracking
        val fitnesses = DoubleArray(genotypes.size)
        cowWorld.entities.forEachIndexed { i, entity ->
            val world = entity.world
            workspace.addUpdateAction("${entity.name} found a flower") {
                (entity.getSensor("centralFlowerSensor") as ObjectSensor).let { sensor ->
                    sensor.getSensedObjects(entity, .5).forEach {
                        it.location = world.getRandomLocation()
                        fitnesses[i] += 1.0
                    }
                }
            }
        }

        workspace.iterateSuspend(iterationsPerRun)

        return if (useAverage) fitnesses.average()
        else fitnesses.min()
    }

    // --- Run evolution ---

    suspend fun runSim() {
        withContext(workspace.coroutineContext) {
            val progressWindow = withGui {
                ProgressWindow(maxGenerations, "10th Percentile Fitness:").apply {
                    minimumSize = Dimension(300, 100)
                    setLocationRelativeTo(null)
                }
            }

            val results = genericEvaluator(
                populationSize = populationSize,
                eliminationRatio = eliminationRatio,
                sortDescending = true,
                create = { List(numCows) { CowBrainGenotype() } },
                copy = { genotypes -> genotypes.map { it.copyGenotype() as CowBrainGenotype } },
                mutate = { genotypes -> genotypes.forEach { it.mutate() } },
                evaluate = ::evaluate,
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

            // Visualize best result
            val bestGenotypes = results.first()
            val cowWorld = buildCowWorld(bestGenotypes, workspace)
            cowWorld.entities.forEachIndexed { i, entity ->
                val world = entity.world
                workspace.addUpdateAction("${entity.name} found a flower") {
                    (entity.getSensor("centralFlowerSensor") as ObjectSensor).let { sensor ->
                        sensor.getSensedObjects(entity, .5).forEach {
                            it.location = world.getRandomLocation()
                        }
                    }
                }
            }
            withGui {
                place(cowWorld.odorWorldComponent, 280, 10, 476, 432)
                cowWorld.networkComponents.forEachIndexed { i, nc ->
                    place(nc, 768, 10 + i * 282, 326, 282)
                }
            }
            bestGenotypes.forEach { g ->
                g.inputs.neurons.location = point(0, 150)
                g.hidden.neurons.location = point(0, 60)
                g.outputs.neurons.location = point(0, -25)
            }
            if (desktop == null) {
                workspace.save(
                    File("evolved_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}.zip"),
                    headless = true
                )
            }

            progressWindow?.close()
        }
    }

    // --- GUI ---

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
                numCows = numCowsTf.text.toInt()
                maxGenerations = maxGenTf.text.toInt()
                iterationsPerRun = iterationsPerRunTf.text.toInt()
                populationSize = populationSizeTf.text.toInt()
                eliminationRatio = eliminationRatioTf.text.toDouble()
                useAverage = useAverageCB.isSelected()
                runSim()
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
