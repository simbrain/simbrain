package org.simbrain.util.geneticalgorithms

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.Events
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Subclasses are genes that express products that can be added to a [Network].
 */
sealed class NetworkGene<P: NetworkModel>: Gene<P>() {
    abstract suspend fun buildWithContext(context: NetworkGeneticsContext): P
}

/**
 * Create a node gene in a chromosome using a template.
 */
class NodeGene private constructor(val template: Neuron = Neuron(null)): NetworkGene<Neuron>() {

    constructor(config: Neuron.() -> Unit): this(Neuron(null)) {
        template.apply(config)
    }

    val fanOut = LinkedHashSet<ConnectionGene>()
    val fanIn = LinkedHashSet<ConnectionGene>()

    override val product = CompletableDeferred<Neuron>()

    val events = NodeGeneEvents()

    fun mutate(config: Neuron.() -> Unit) {
        template.apply(config)
    }

    override fun copy(): NodeGene {
        val newGene = NodeGene(template.deepCopy())
        events.copy.fireAndForget(newGene)
        return newGene
    }

    override suspend fun buildWithContext(context: NetworkGeneticsContext): Neuron = completeWith {
      Neuron(context.network, template)
    }

}

class NodeGeneEvents: Events() {
    val copy = AddedEvent<NodeGene>()
}


class ConnectionGene(private val template: Synapse, val source: NodeGene, val target: NodeGene) : NetworkGene<Synapse>() {

    override val product: CompletableDeferred<Synapse> = CompletableDeferred()

    private lateinit var sourceCopy: NodeGene
    private lateinit var targetCopy: NodeGene

    init {
        source.fanOut.add(this)
        target.fanIn.add(this)
        source.events.copy.on { sourceCopy = it }
        target.events.copy.on { targetCopy = it }
    }

    fun mutate(block: Synapse.() -> Unit) {
        template.apply(block)
    }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(template), sourceCopy, targetCopy)
    }

    override suspend fun buildWithContext(context: NetworkGeneticsContext): Synapse {
        return withTimeout(1000) {
            Synapse(context.network, source.product.await(), target.product.await(), template.learningRule, template)
                .also {
                    context.network.addNetworkModelAsync(it)
                    product.complete(it)
                }
        }
    }

}

/**
 * Needed so we can evolve different types of layout.
 */
class LayoutWrapper(var layout: Layout, var hSpacing: Double, var vSpacing: Double): CopyableObject {
    override fun copy() = LayoutWrapper(layout.copy(), hSpacing, vSpacing)
}

class LayoutGene(private val template: LayoutWrapper) : Gene<Layout>(), TopLevelGene<Layout> {

    override val product = CompletableDeferred<Layout>()

    // Todo: handle types of layout

    override fun copy(): LayoutGene {
        return LayoutGene(template.copy())
    }

    fun mutate(config: LayoutWrapper.() -> Unit) {
        template.apply(config)
    }

    override suspend fun TopLevelBuilderContext.build(): Layout = completeWith {
        template.layout.apply {
            when(this) {
                is GridLayout -> {
                    hSpacing = template.hSpacing
                    vSpacing = template.vSpacing
                }
                is HexagonalGridLayout -> {
                    hSpacing = template.hSpacing
                    vSpacing = template.vSpacing
                }
                is LineLayout -> {
                    spacing = when (orientation) {
                        LineLayout.LineOrientation.VERTICAL -> template.vSpacing
                        LineLayout.LineOrientation.HORIZONTAL -> template.hSpacing
                    }
                }
            }
        }
    }

}

/**
 * Provides DSL helpers for network genetics.
 */
class NetworkGeneticsContext(val network: Network) {

    suspend fun <P, G: NetworkGene<P>> express(chromosome: Chromosome<P, G>): List<P> = chromosome.map {
        it.buildWithContext(this).also { product ->
            if (product is Neuron) network.addNetworkModelAsync(product)
        }
    }

    suspend operator fun <P, G: NetworkGene<P>> Chromosome<P, G>.unaryPlus(): List<P> = express(this)

    suspend fun Chromosome<Neuron, NodeGene>.asGroup(block: NeuronGroup.() -> Unit = { }): (suspend (network: Network) -> NeuronGroup) {
        return { network ->
            map { it.buildWithContext(this@NetworkGeneticsContext) }
                .let { NeuronGroup(network, it).apply(block) }
                .also { network.addNetworkModelAsync(it) }
        }
    }

    fun Chromosome<Neuron, NodeGene>.asNeuronCollection(block: NeuronCollection.() -> Unit = { }): (suspend (network: Network) -> NeuronCollection) {
        return { network ->
            map { it.buildWithContext(this@NetworkGeneticsContext) }
                .let {
                    it.forEach(network::addNetworkModelAsync)
                    NeuronCollection(network, it).apply(block) }
                .also { network.addNetworkModelAsync(it) }
        }
    }

    suspend operator fun <T> (suspend (Network) -> T).unaryPlus(): T = this(network)

}

suspend operator fun Network.invoke(block: suspend NetworkGeneticsContext.() -> Unit) = coroutineScope {
    NetworkGeneticsContext(this@invoke).apply { block() }
}


/**
 * Helper functions to create genes
 */
fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene {
    return NodeGene(options)
}
inline fun connectionGene(source: NodeGene, target: NodeGene, options: Synapse.() -> Unit = { }): ConnectionGene {
    return ConnectionGene(Synapse(null, null as Neuron?).apply(options), source, target)
}
inline fun layoutGene(options: GridLayout.() -> Unit = { }): LayoutGene {
    val layout = GridLayout().apply(options)
    return LayoutGene(LayoutWrapper(layout, layout.hSpacing, layout.vSpacing))
}
