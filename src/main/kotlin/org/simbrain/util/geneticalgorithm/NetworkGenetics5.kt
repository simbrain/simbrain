package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import java.util.*

inline fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene5 {
    return NodeGene5(Neuron(null).apply(options))
}

inline fun connectionGene(source: NodeGene5, target: NodeGene5, options: Synapse.() -> Unit = { }): ConnectionGene5 {
    return ConnectionGene5(Synapse(null, null as Neuron?).apply(options), source, target)
}

class NodeGene5 (template: Neuron) : Gene5<Neuron>(template) {

    private val copyListeners = LinkedList<(NodeGene5) -> Unit>()

    fun onCopy(task: (NodeGene5) -> Unit) {
        copyListeners.add(task)
    }

    private fun fireCopied(newGene: NodeGene5) {
        copyListeners.forEach { it(newGene) }
    }

    override fun copy(): NodeGene5 {
        val newGene = NodeGene5(template.deepCopy())
        fireCopied(newGene)
        return newGene
    }

    fun build(network: Network): Neuron {
        return Neuron(network, template)
    }

}

class ConnectionGene5 (template: Synapse, val source: NodeGene5, val target: NodeGene5) : Gene5<Synapse>(template) {

    lateinit var sourceCopy: NodeGene5
    lateinit var targetCopy: NodeGene5

    init {
        source.onCopy { sourceCopy = it }
        target.onCopy { targetCopy = it }
    }

    override fun copy(): ConnectionGene5 {
        return ConnectionGene5(Synapse(template), sourceCopy, targetCopy)
    }

    fun build(network: Network, source: Neuron, target: Neuron): Synapse {
        return Synapse(network, source, target, template.learningRule, template)
    }

}

