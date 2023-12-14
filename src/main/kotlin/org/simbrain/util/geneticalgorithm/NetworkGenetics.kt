package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.util.sampleOne
import org.simbrain.util.toRatio
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

    override suspend fun express(network: Network) = Neuron(network, template).also {
        network.addNetworkModelAsync(it)
        expressedNeuron.complete(it)
    }

    override fun copy(): NodeGene {
        return NodeGene(template.deepCopy()).also { listeners.forEach { l -> l(it) } }
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
            Synapse(network, source, target, template.learningRule, template).also { network.addNetworkModelAsync(it) }
        }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(template), copiedSource, copiedTarget)
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

class NeuronRuleGeneWrapper(var updateRule: NeuronUpdateRule<*, *>) {
    fun copy() = NeuronRuleGeneWrapper(updateRule.copy())
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

fun nodeGene(block: Neuron.() -> Unit = {}) = NodeGene(template = Neuron(null)).apply { template.block() }

fun connectionGene(source: NodeGene, target: NodeGene, block: Synapse.() -> Unit = {}) = ConnectionGene(
    template = Synapse(null as Neuron?, null),
    source, target
).apply { template.block() }

fun layoutGene(block: LayoutGeneWrapper.() -> Unit = {}) =
    LayoutGene(template = LayoutGeneWrapper()).apply { template.block() }

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

fun neuronRuleGene(initialRule: NeuronUpdateRule<*, *> = LinearRule(), block: NeuronRuleGeneWrapper.() -> Unit = {}) =
    NeuronRuleGene(template = NeuronRuleGeneWrapper(initialRule)).apply { template.block() }

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
    allowedTypes: List<Pair<Number, KClass<out NeuronUpdateRule<*, *>>>> = allUpdateRules.map { 1 to it.kotlin },
    probabilityOfChange: Double = .9
) = mutate {
    val nonMutatingWeight: Double = (1 - probabilityOfChange).toRatio() * allowedTypes.size
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