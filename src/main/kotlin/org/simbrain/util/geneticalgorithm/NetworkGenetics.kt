package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

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

fun nodeGene(block: Neuron.() -> Unit = {}) = NodeGene(template = Neuron(null)).apply { template.block() }

fun connectionGene(source: NodeGene, target: NodeGene, block: Synapse.() -> Unit = {}) = ConnectionGene(
    template = Synapse(null as Neuron?, null),
    source, target
).apply { template.block() }