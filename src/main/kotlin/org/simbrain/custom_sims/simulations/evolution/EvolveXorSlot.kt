package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.activations
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.sse
import org.simbrain.workspace.Workspace
import kotlin.random.Random

// ======================================================================
// EvoSim builder (prototype — not yet in core)
//
// Eliminates per-simulation EvoSim classes for simple single-genotype sims.
// ======================================================================

class EvoSimScope<G : SlotGenotype>(
    val genotype: G,
    val workspace: Workspace,
    internal val factory: EvoSimFactory<G>
) {
    val networkComponents = mutableListOf<NetworkComponent>()

    internal var buildBlock: (suspend EvoSimScope<G>.() -> Unit)? = null
    internal var evalBlock: (suspend EvoSimScope<G>.() -> Double)? = null

    fun addNetworkComponent(name: String): NetworkComponent {
        return NetworkComponent(name).also {
            workspace.addWorkspaceComponent(it)
            networkComponents.add(it)
        }
    }

    fun build(block: suspend EvoSimScope<G>.() -> Unit) {
        buildBlock = block
    }

    fun eval(block: suspend EvoSimScope<G>.() -> Double) {
        evalBlock = block
    }
}

class EvoSimInstance<G : SlotGenotype>(val scope: EvoSimScope<G>) : EvoSim {

    val genotype: G get() = scope.genotype
    val workspace: Workspace get() = scope.workspace
    private var built = false

    override fun mutate() = scope.genotype.mutate()

    override suspend fun build() {
        if (!built) {
            scope.buildBlock?.invoke(scope)
            built = true
        }
    }

    override fun copy(): EvoSim {
        @Suppress("UNCHECKED_CAST")
        val newGenotype = scope.genotype.copyGenotype() as G
        return scope.factory.createFrom(newGenotype)
    }

    override fun visualize(workspace: Workspace): EvoSim {
        @Suppress("UNCHECKED_CAST")
        val newGenotype = scope.genotype.copyGenotype() as G
        return scope.factory.createFrom(newGenotype, workspace)
    }

    override suspend fun eval(): Double {
        build()
        return scope.evalBlock?.invoke(scope) ?: error("No eval block registered")
    }
}

class EvoSimFactory<G : SlotGenotype>(
    private val genotypeFactory: (Long) -> G,
    private val setup: EvoSimScope<G>.() -> Unit
) {
    fun create(seed: Long): EvoSimInstance<G> {
        return createFrom(genotypeFactory(seed))
    }

    fun createFrom(genotype: G, workspace: Workspace = Workspace()): EvoSimInstance<G> {
        val scope = EvoSimScope(genotype, workspace, this)
        scope.setup()
        return EvoSimInstance(scope)
    }
}

fun <G : SlotGenotype> evoSim(
    genotypeFactory: (Long) -> G,
    setup: EvoSimScope<G>.() -> Unit
): EvoSimFactory<G> = EvoSimFactory(genotypeFactory, setup)

// ======================================================================
// XOR Simulation — Slot DSL
// ======================================================================

val evolveXorSlot = newSim {

    val evaluatorParams = EvaluatorParams(
        populationSize = 100,
        eliminationRatio = 0.5,
        targetMetric = 0.01,
        stoppingCondition = EvaluatorParams.StoppingCondition.Error,
        maxGenerations = 500,
        iterationsPerRun = 2,
        seed = 42
    )

    class XorGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        val inputs by nodeChromosome(2) { clamped = true; upperBound = 1.0; lowerBound = -1.0 }
        val hidden by nodeChromosome(2) { upperBound = 1.0; lowerBound = -1.0 }
        val output by nodeChromosome(1) { upperBound = 1.0; lowerBound = -1.0 }
        val connections by connectionChromosome()

        init {
            connections.addConnection(inputs to hidden) { strength = random.nextDouble(-1.0, 1.0) }
            connections.addConnection(hidden to output) { strength = random.nextDouble(-1.0, 1.0) }
        }

        override fun createNew(seed: Long) = XorGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach { it.mutate { bias += random.nextDouble(-1.0, 1.0) } }
            connections.genes.forEach { it.mutate { strength += random.nextDouble(-1.0, 1.0) } }

            withProbability(0.25) {
                connections.addConnection(inputs to hidden, hidden to output) {
                    strength = random.nextDouble(-1.0, 1.0)
                }
            }

            if (random.nextDouble() < 0.1) {
                hidden.chromosome.add(nodeGene())
            }
        }
    }

    val simFactory = evoSim(::XorGenotype) {
        val networkComponent = addNetworkComponent("network 1")
        val network = networkComponent.network

        build {
            genotype.expressAll(network)
            genotype.inputs.neurons.label = "input"
            genotype.hidden.neurons.label = "hidden"
            genotype.output.neurons.label = "output"
        }

        eval {
            val testData = listOf(
                listOf(0.0, 0.0) to listOf(0.0),
                listOf(0.0, 1.0) to listOf(1.0),
                listOf(1.0, 0.0) to listOf(1.0),
                listOf(1.0, 1.0) to listOf(0.0)
            )
            testData.sumOf { (input, expected) ->
                genotype.inputs.neurons.neuronList.activations = input
                workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
                genotype.output.neurons.neuronList.activations sse expected
            }
        }
    }

    suspend fun runSim() {
        val lastGeneration = evaluator(evaluatorParams, populatingFunction = { simFactory.create(seed) })
        val best = lastGeneration.first().visualize(workspace) as EvoSimInstance<XorGenotype>
        best.build()
        best.genotype.inputs.neurons.neuronList.forEach { n -> n.increment = 1.0 }
        best.genotype.inputs.neurons.location = point(0, 150)
        best.genotype.hidden.neurons.location = point(0, 60)
        best.genotype.output.neurons.location = point(0, -25)
        withGui {
            place(best.scope.networkComponents.first(), 340, 10, 384, 480)
        }
    }

    withGui {
        workspace.clearWorkspace()
        evaluatorParams.createControlPanel("Control Panel", 5, 10)
        evaluatorParams.addControlPanelButton("Evolve") {
            workspace.removeAllComponents()
            evaluatorParams.addProgressWindow()
            runSim()
        }
    }

    if (it?.isNotEmpty() == true) {
        runSim()
    }
}

// ======================================================================
// Paired XOR — tests linked chromosomes (neuronRuleChromosome, synapseRuleChromosome)
// ======================================================================

val evolveXorPaired = newSim {

    val evaluatorParams = EvaluatorParams(
        populationSize = 50,
        eliminationRatio = 0.5,
        targetMetric = 0.1,
        stoppingCondition = EvaluatorParams.StoppingCondition.Error,
        maxGenerations = 200,
        iterationsPerRun = 2,
        seed = 42
    )

    class PairedXorGenotype(seed: Long = Random.nextLong()) : SlotGenotype(seed) {

        val inputs by nodeChromosome(2) { clamped = true; upperBound = 1.0; lowerBound = -1.0 }
        val hidden by nodeChromosome(2) { upperBound = 1.0; lowerBound = -1.0 }
        val output by nodeChromosome(1) { upperBound = 1.0; lowerBound = -1.0 }
        val connections by connectionChromosome()

        val hiddenRules by neuronRuleChromosome(::hidden)
        val connectionRules by synapseRuleChromosome(::connections)

        init {
            connections.addConnection(inputs to hidden) { strength = random.nextDouble(-1.0, 1.0) }
            connections.addConnection(hidden to output) { strength = random.nextDouble(-1.0, 1.0) }
        }

        override fun createNew(seed: Long) = PairedXorGenotype(seed)

        override fun mutate() {
            hidden.genes.forEach { it.mutate { bias += random.nextDouble(-1.0, 1.0) } }
            connections.genes.forEach { it.mutate { strength += random.nextDouble(-1.0, 1.0) } }

            connectionRules.genes.forEach { it.mutateParam() }
            hiddenRules.genes.forEach { it.mutateParam(mutateBounds = false) }

            withProbability(0.25) {
                connections.addConnection(inputs to hidden, hidden to output) {
                    strength = random.nextDouble(-1.0, 1.0)
                }
            }

            if (random.nextDouble() < 0.1) {
                hidden.addGene(nodeGene())
            }
        }
    }

    val simFactory = evoSim(::PairedXorGenotype) {
        val networkComponent = addNetworkComponent("network 1")
        val network = networkComponent.network

        build {
            genotype.expressAll(network)
            genotype.inputs.neurons.label = "input"
            genotype.hidden.neurons.label = "hidden"
            genotype.output.neurons.label = "output"
        }

        eval {
            val testData = listOf(
                listOf(0.0, 0.0) to listOf(0.0),
                listOf(0.0, 1.0) to listOf(1.0),
                listOf(1.0, 0.0) to listOf(1.0),
                listOf(1.0, 1.0) to listOf(0.0)
            )
            testData.sumOf { (input, expected) ->
                genotype.inputs.neurons.neuronList.activations = input
                workspace.iterateSuspend(evaluatorParams.iterationsPerRun)
                genotype.output.neurons.neuronList.activations sse expected
            }
        }
    }

    suspend fun runSim() {
        val lastGeneration = evaluator(evaluatorParams, populatingFunction = { simFactory.create(seed) })
        println("Best error: ${lastGeneration.first().eval()}")
        println("Hidden neurons: ${(lastGeneration.first() as EvoSimInstance<PairedXorGenotype>).genotype.hidden.genes.size}")
        println("Connections: ${(lastGeneration.first() as EvoSimInstance<PairedXorGenotype>).genotype.connections.genes.size}")
    }

    if (it?.isNotEmpty() == true) {
        runSim()
    }
}
