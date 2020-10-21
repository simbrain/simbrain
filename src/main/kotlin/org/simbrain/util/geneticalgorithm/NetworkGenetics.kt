package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import java.util.*

inline fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene {
    return NodeGene(Neuron(null).apply(options))
}

inline fun connectionGene(source: NodeGene, target: NodeGene, options: Synapse.() -> Unit = { }): ConnectionGene {
    return ConnectionGene(Synapse(null, null as Neuron?).apply(options), source, target)
}

class NodeGene (template: Neuron) : Gene<Neuron>(template) {

    private val copyListeners = LinkedList<(NodeGene) -> Unit>()

    fun onCopy(task: (NodeGene) -> Unit) {
        copyListeners.add(task)
    }

    private fun fireCopied(newGene: NodeGene) {
        copyListeners.forEach { it(newGene) }
    }

    override fun copy(): NodeGene {
        val newGene = NodeGene(template.deepCopy())
        fireCopied(newGene)
        return newGene
    }

    fun build(network: Network): Neuron {
        return Neuron(network, template)
    }

}

class ConnectionGene (template: Synapse, val source: NodeGene, val target: NodeGene) : Gene<Synapse>(template) {

    lateinit var sourceCopy: NodeGene
    lateinit var targetCopy: NodeGene

    init {
        source.onCopy { sourceCopy = it }
        target.onCopy { targetCopy = it }
    }

    override fun copy(): ConnectionGene {
        return ConnectionGene(Synapse(template), sourceCopy, targetCopy)
    }

    fun build(network: Network, source: Neuron, target: Neuron): Synapse {
        return Synapse(network, source, target, template.learningRule, template)
    }

}

