package org.simbrain.util.geneticalgorithm2

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse

interface NetworkGene2<P : NetworkModel> : Gene2<P> {
    suspend fun express(network: Network): P

}

class NodeGene2(override val template: Neuron) : NetworkGene2<Neuron> {

    private val _expressedNeuron = CompletableDeferred<Neuron>()

    val expressedNeuron by this::_expressedNeuron

    private val listeners = mutableListOf<(NodeGene2) -> Unit>()
    fun onCopied(block: (NodeGene2) -> Unit) {
        listeners.add(block)
    }

    override suspend fun express(network: Network) = Neuron(network, template).also {
        network.addNetworkModel(it)
        expressedNeuron.complete(it)
    }

    override fun copy(): NodeGene2 {
        return NodeGene2(template.deepCopy()).also { listeners.forEach { l -> l(it) } }
    }

    fun mutate(mutation: Neuron.() -> Unit) {
        template.apply(mutation)
    }
}

class ConnectionGene2(override val template: Synapse, val source: NodeGene2, val target: NodeGene2) :
    NetworkGene2<Synapse> {

    private lateinit var copiedSource: NodeGene2
    private lateinit var copiedTarget: NodeGene2

    init {
        source.onCopied { copiedSource = it }
        target.onCopied { copiedTarget = it }
    }

    override suspend fun express(network: Network) =
        with(withTimeout(1000) { source.expressedNeuron.await() } to withTimeout(1000) { target.expressedNeuron.await() }) {
            val (source, target) = this
            Synapse(network, source, target, template.learningRule, template).also { network.addNetworkModel(it) }
        }

    override fun copy(): ConnectionGene2 {
        return ConnectionGene2(Synapse(template), copiedSource, copiedTarget)
    }

    fun mutate(mutation: Synapse.() -> Unit) {
        template.apply(mutation)
    }
}

fun nodeGene2(block: Neuron.() -> Unit = {}) = NodeGene2(template = Neuron(null)).apply { template.block() }

fun connectionGene2(source: NodeGene2, target: NodeGene2, block: Synapse.() -> Unit = {}) = ConnectionGene2(
    template = Synapse(null as Neuron?, null),
    source, target
).apply { template.block() }