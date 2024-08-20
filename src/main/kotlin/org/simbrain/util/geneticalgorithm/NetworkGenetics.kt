package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.simbrain.network.connections.*
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.learningrules.OjaRule
import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.updaterules.*
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.util.cartesianProduct
import org.simbrain.util.sampleOne
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

abstract class NetworkGene<P : NetworkModel> : Gene<P>() {
    abstract suspend fun express(network: Network): P

}

class NodeGene(override val template: Neuron) : NetworkGene<Neuron>() {

    private val _expressedNeuron = CompletableDeferred<Neuron>()

    val expressedNeuron by this::_expressedNeuron

    private val listeners = mutableListOf<(NodeGene) -> Unit>()
    fun onCopied(block: (NodeGene) -> Unit) {
        listeners.add(block)
    }

    override suspend fun express(network: Network) = Neuron(template).also {
        network.addNetworkModel(it)
        expressedNeuron.complete(it)
    }

    override fun copy(): NodeGene {
        return NodeGene(template.copy()).also { listeners.forEach { l -> l(it) } }
    }

}

/**
 * Describes a synapse connecting two neurons, associated with nodegenes.  When expressed, will wait until the
 * associated node genes are expressed first.
 */
class ConnectionGene(override val template: Synapse, val source: NodeGene, val target: NodeGene) :
    NetworkGene<Synapse>() {

    private lateinit var copiedSource: NodeGene
    private lateinit var copiedTarget: NodeGene

    init {
        source.onCopied { copiedSource = it }
        target.onCopied { copiedTarget = it }
    }

    override suspend fun express(network: Network) =
        with(withTimeout(1000) { source.expressedNeuron.await() } to withTimeout(1000) { target.expressedNeuron.await() }) {
            val (source, target) = this
            Synapse(source, target, template).also { network.addNetworkModel(it) }
        }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(source.template, target.template, template), copiedSource, copiedTarget)
    }
}

data class LayoutGeneWrapper(var layoutType: Layout = GridLayout(), var hSpacing: Double = GridLayout.DEFAULT_H_SPACING, var vSpacing: Double = GridLayout.DEFAULT_V_SPACING) {
    fun express() = layoutType.copy().also {
        when (it) {
            is GridLayout -> {
                it.hSpacing = hSpacing
                it.vSpacing = vSpacing
            }
            is HexagonalGridLayout -> {
                it.hSpacing = hSpacing
                it.vSpacing = vSpacing
            }
            is LineLayout -> when (it.orientation) {
                LineLayout.LineOrientation.HORIZONTAL -> it.spacing = hSpacing
                LineLayout.LineOrientation.VERTICAL -> it.spacing = vSpacing
            }
        }
    }
    fun copy() = LayoutGeneWrapper(layoutType.copy(), hSpacing, vSpacing)
}

class ConnectionStrategyGeneWrapper(var connectionStrategy: ConnectionStrategy) {
    fun copy() = ConnectionStrategyGeneWrapper(connectionStrategy.copy())
}

class NeuronRuleGeneWrapper(var updateRule: NeuronUpdateRule<*, *>) {
    fun copy() = NeuronRuleGeneWrapper(updateRule.copy())
}

class SynapseRuleGeneWrapper(var learningRule: SynapseUpdateRule<*, *>) {
    fun copy() = SynapseRuleGeneWrapper(learningRule.copy())
}

class LayoutGene(override val template: LayoutGeneWrapper) : TopLevelGene<LayoutGeneWrapper>() {

    private val _expressedLayout = CompletableDeferred<LayoutGeneWrapper>()

    val expressedLayout by this::_expressedLayout

    override fun express() = template.copy().also {
        expressedLayout.complete(it)
    }

    override fun copy(): LayoutGene {
        return LayoutGene(template.copy())
    }

}

class ConnectionStrategyGene(override val template: ConnectionStrategyGeneWrapper) : TopLevelGene<ConnectionStrategyGeneWrapper>() {

    private val _expressedConnectionStrategy = CompletableDeferred<ConnectionStrategyGeneWrapper>()

    val expressedConnectionStrategy by this::_expressedConnectionStrategy

    override fun express() = template.copy().also {
        expressedConnectionStrategy.complete(it)
    }

    override fun copy(): ConnectionStrategyGene {
        return ConnectionStrategyGene(template.copy())
    }

}

class NeuronRuleGene(override val template: NeuronRuleGeneWrapper) : TopLevelGene<NeuronRuleGeneWrapper>() {

    private val _expressedNeuronRule = CompletableDeferred<NeuronRuleGeneWrapper>()

    val expressedNeuronRule by this::_expressedNeuronRule

    override fun express() = template.copy().also {
        expressedNeuronRule.complete(it)
    }

    override fun copy(): NeuronRuleGene {
        return NeuronRuleGene(template.copy())
    }

}

class SynapseRuleGene(override val template: SynapseRuleGeneWrapper) : TopLevelGene<SynapseRuleGeneWrapper>() {

    private val _expressedSynapseRule = CompletableDeferred<SynapseRuleGeneWrapper>()

    val expressedSynapseRule by this::_expressedSynapseRule

    override fun express() = template.copy().also {
        expressedSynapseRule.complete(it)
    }

    override fun copy(): SynapseRuleGene {
        return SynapseRuleGene(template.copy())
    }

}

fun nodeGene(block: Neuron.() -> Unit = {}) = NodeGene(template = Neuron()).apply { template.block() }

fun connectionGene(source: NodeGene, target: NodeGene, block: Synapse.() -> Unit = {}) = ConnectionGene(
    template = Synapse(source.template, target.template),
    source, target
).apply { template.block() }

fun layoutGene(block: LayoutGeneWrapper.() -> Unit = {}) =
    LayoutGene(template = LayoutGeneWrapper()).apply { template.block() }

fun connectionStrategyGene(block: ConnectionStrategyGeneWrapper.() -> Unit = {}) =
    ConnectionStrategyGene(template = ConnectionStrategyGeneWrapper(Sparse())).apply { template.block() }

fun neuronRuleGene(initialRule: NeuronUpdateRule<*, *> = LinearRule(), block: NeuronRuleGeneWrapper.() -> Unit = {}) =
    NeuronRuleGene(template = NeuronRuleGeneWrapper(initialRule)).apply { template.block() }

fun synapseRuleGene(initialRule: SynapseUpdateRule<*, *> = StaticSynapseRule(), block: SynapseRuleGeneWrapper.() -> Unit = {}) =
    SynapseRuleGene(template = SynapseRuleGeneWrapper(initialRule)).apply { template.block() }

context(Genotype)
fun LayoutGene.mutateParam() = mutate {
    hSpacing += random.nextDouble(-1.0, 1.0)
    vSpacing += random.nextDouble(-1.0, 1.0)
}

context(Genotype)
fun LayoutGene.mutateType() = mutate {
    when (random.nextDouble()) {
        in 0.0..0.5 -> layoutType = GridLayout()
        in 0.5..1.0 -> layoutType = HexagonalGridLayout()
        // in 0.1..0.15 -> layout = LineLayout()
    }
}

context(Genotype)
fun ConnectionStrategyGene.mutateParam(changeProbability: Double = .1,) = mutate {
    random.nextDouble().let {
        if (it < changeProbability) {
            with(connectionStrategy) {
                // Sparse: connection density
                if (this is Sparse) {
                    random.mutateProperty(::connectionDensity, delta = 0.1)
                }
                // RadialProbabilistic: excitatoryProbability, inhibitoryProbability, excitatoryRadius, inhibitoryRadius
                if (this is RadialProbabilistic) {
                    random.mutateProperty(::excitatoryProbability, delta = 0.1)
                    random.mutateProperty(::inhibitoryProbability, delta = 0.1)
                    random.mutateProperty(::excitatoryRadius, delta = 0.1)
                    random.mutateProperty(::inhibitoryRadius, delta = 0.1)
                }
                // FixedDegree: degree, direction
                if (this is FixedDegree) {
                    random.mutateProperty(::degree, delta = 1)
                    random.nextBoolean().let { direction = if (it) Direction.IN else Direction.OUT }
                }
                if (this is RadialGaussian) {
                    random.mutateProperty(::distConst, delta = 0.1)
                }
            }
        }
    }
}

context(Genotype)
fun ConnectionStrategyGene.mutateType(
    allowedTypes: List<Pair<Number, KClass<out ConnectionStrategy>>> = (
            connectionTypes -
                    setOf(
                        AllToAll::class.java,
                        DistanceBased::class.java,
                        OneToOne::class.java
                    )
            ).map { 1 to it.kotlin },
    probabilityOfChange: Double = .9
) = mutate {
    val nonMutatingWeight: Double = (1 - probabilityOfChange).toProbabilityWeight() * allowedTypes.size
    fun changeIfNotSameType(newType: KClass<out ConnectionStrategy>) {
        if (connectionStrategy::class != newType) {
            connectionStrategy = newType.createInstance()
        }
    }
    random.runOne(
        nonMutatingWeight to {  },
        *allowedTypes.map { (weight, type) -> weight to { changeIfNotSameType(type) } }.toTypedArray()
    )
}

context(Genotype)
fun NeuronRuleGene.mutateParam(
    changeProbability: Double = .1,
    mutateNoise:Boolean = true,
    mutateBounds:Boolean  = true
) = mutate {
    with(updateRule) {

        if (mutateNoise && this is NoisyUpdateRule) {
            addNoise = random.nextBoolean()
        }

        if (mutateBounds && this is BoundedUpdateRule) {
            lowerBound += random.nextDouble(-1.0, -0.2)
            upperBound += random.nextDouble(0.2, 1.0)
        }

        // Util for changing a parameter
        fun changeParam(block: () -> Unit) {
            if (random.nextDouble() < changeProbability) {
                block()
            }
        }

        // TODO: More cases
        when (this) {
            is LinearRule -> {
                changeParam{clippingType = LinearRule.ClippingType.entries.sampleOne()}
            }
            is BinaryRule -> {
                changeParam { threshold += random.nextDouble(-1.0, 1.0) }
            }
            is DecayRule -> {
                changeParam {decayAmount += random.nextDouble(-1.0, 1.0)}
            }
        }

    }
}

/**
 * Mutate between one of the provided types of Neuron rule (default is all of them).
 *
 * @param probabilityOfChange change to one of the allowed types (with equal weighting between them) with this probability
 */
context(Genotype)
fun NeuronRuleGene.mutateType(
    allowedTypes: List<Pair<Number, KClass<out NeuronUpdateRule<*, *>>>> = scalarUpdateRules.map { 1 to it.kotlin },
    probabilityOfChange: Double = .9
) = mutate {
    val nonMutatingWeight: Double = (1 - probabilityOfChange).toProbabilityWeight() * allowedTypes.size
    fun changeIfNotSameType(newType: KClass<out NeuronUpdateRule<*, *>>) {
        if (updateRule::class != newType) {
            updateRule = newType.createInstance()
        }
    }
    random.runOne(
        nonMutatingWeight to {  },
        *allowedTypes.map { (weight, type) -> weight to { changeIfNotSameType(type) } }.toTypedArray()
    )
}

context(Genotype)
fun NeuronRuleGene.mutateStandardTypes() = mutateType(
    allowedTypes = listOf(1 to LinearRule::class, 1 to SigmoidalRule::class, 1 to BinaryRule::class, 1 to SigmoidalRule::class),
)

context(Genotype)
fun SynapseRuleGene.mutateParam(
    changeProbability: Double = .1,
) = mutate {
    random.nextDouble().let {
        if (it < changeProbability) {
            with(learningRule) {
                // Hebbian: learningRate
                if (this is HebbianRule) {
                    random.nextDouble(-0.1, 0.1).let { delta ->
                        learningRate += delta
                        if (learningRate < 0) {
                            learningRate = 0.0
                        }
                    }
                }
                // Oja: learningRate
                if (this is OjaRule) {
                    random.mutateProperty(::learningRate, delta = 0.1)
                }
            }
        }
    }
}

context(Genotype)
fun SynapseRuleGene.mutateType(
    allowedTypes: List<Pair<Number, KClass<out SynapseUpdateRule<*, *>>>> = listOf(
        1 to StaticSynapseRule::class,
        1 to HebbianRule::class,
        1 to OjaRule::class,
    ),
    probabilityOfChange: Double = .9
) = mutate {
    val nonMutatingWeight: Double = (1 - probabilityOfChange).toProbabilityWeight() * allowedTypes.size
    fun changeIfNotSameType(newType: KClass<out SynapseUpdateRule<*, *>>) {
        if (learningRule::class != newType) {
            learningRule = newType.createInstance()
        }
    }
    random.runOne(
        nonMutatingWeight to {  },
        *allowedTypes.map { (weight, type) -> weight to { changeIfNotSameType(type) } }.toTypedArray()
    )
}


/**
 * Creates a connection gene between two groups of node genes.
 * Provide one or more lists of possible connections to choose from.
 * Ex: input to hidden, hidden to input, input + hidden to output.
 */
context(Genotype)
fun Chromosome<Synapse, ConnectionGene>.createGene(
    vararg sourceTargetChromosomeGroups: Pair<Chromosome<Neuron, NodeGene>, Chromosome<Neuron, NodeGene>>,
    synapseGeneTemplate: Synapse.() -> Unit = {}
): ConnectionGene? {
    // Ensure existing connections are not used when creating new connections
    val existingConnections = this.map { it.source to it.target }.toSet()
    val availableConnections = sourceTargetChromosomeGroups.flatMap { (sources, targets) ->
        sources cartesianProduct targets
    }.toSet()
    val availableConnectionsNotInExisting = (availableConnections - existingConnections).toList()
    if (availableConnectionsNotInExisting.isNotEmpty()) {
        val (source, target) = availableConnectionsNotInExisting.sampleOne(random)
        return connectionGene(source, target, synapseGeneTemplate).also { add(it) }
    } else {
        return null
    }
}