package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout

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