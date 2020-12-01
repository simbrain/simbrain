package org.simbrain.util.geneticalgorithms

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import java.util.*
import kotlin.collections.ArrayList

inline fun nodeGene(options: Neuron.() -> Unit = { }): NodeGene {
    return NodeGene(Neuron(null).apply(options))
}

inline fun connectionGene(source: NodeGene, target: NodeGene, options: Synapse.() -> Unit = { }): ConnectionGene {
    return ConnectionGene(Synapse(null, null as Neuron?).apply(options), source, target)
}

sealed class NetworkGene<T>(template: T): Gene<T>(template)

class NodeGene(template: Neuron) : NetworkGene<Neuron>(template), ProducerGene<Neuron>, ConsumerGene<Neuron> {

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

    override fun CouplingManager.defaultProducer(container: Neuron): Producer? {
        return container.getProducer("getActivation")
    }

    override fun CouplingManager.defaultConsumer(container: Neuron): Consumer? {
        return container.getConsumer("setInputValue")
    }

    fun build(network: Network): Neuron {
        return Neuron(network, template)
    }

}

class ConnectionGene(template: Synapse, val source: NodeGene, val target: NodeGene) : NetworkGene<Synapse>(template) {

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

class NetworkBuilderProvider: BuilderProvider<Network, NetworkGeneticBuilder, NetworkBuilderContext>,
        WorkspaceBuilderContextInvokable<NetworkBuilderContext, Network>,
        TopLevelBuilderContextInvokable<NetworkBuilderContext, Network> {

    private lateinit var product: Network

    override fun createWorkspaceComponent(name: String) = NetworkComponent(name, product)

    fun createBuilder(productMap: ProductMap): NetworkGeneticBuilder {
        return NetworkGeneticBuilder(productMap)
    }

    fun createContext(builder: NetworkGeneticBuilder): NetworkBuilderContext {
        return NetworkBuilderContext(builder)
    }

    override fun createProduct(productMap: ProductMap, template: NetworkBuilderContext.() -> Unit): Network {
        return createBuilder(productMap).also { createContext(it).apply(template) }.build().also { product = it }
    }

}

class NetworkGeneticBuilder(override val productMap: ProductMap) : GeneticBuilder<Network> {

    val tasks = ArrayList<(Network) -> Unit>()

    fun build(): Network {
        return Network().also { net -> tasks.forEach { it(net) } }
    }

}

class NetworkBuilderContext(val builder: NetworkGeneticBuilder): BuilderContext {

    private inline fun <C: Chromosome<T, G>, G: NetworkGene<T>, T> C.addGene(
            crossinline adder: (gene: G, net: Network) -> T
    ) {
        builder.tasks.add { net ->
            genes.forEach {
                builder.productMap[it] = adder(it, net)
            }
        }
    }

    operator fun ((Network) -> Unit).unaryPlus() {
        builder.tasks.add(this)
    }

    @JvmName("unaryPlusNeuron")
    operator fun <C: Chromosome<Neuron, NodeGene>> C.unaryPlus() {
        addGene { gene, net ->
            gene.build(net).also { neuron ->
                net.addLooseNeuron(neuron)
            }
        }
    }

    private inline fun <T, G: Gene<T>, C: Chromosome<T, G>> C.option(
            crossinline options: List<T>.() -> Unit,
            crossinline adder: (gene: G, net: Network) -> T
    ): (Network) -> Unit {
        return { net ->
            genes.map { gene ->
                adder(gene, net).also { builder.productMap[gene] = it }
            }.options()
        }
    }

    operator fun <C: Chromosome<Neuron, NodeGene>> C.invoke(options: List<Neuron>.() -> Unit) =
            option(options) { gene, net ->
                gene.build(net).also { neuron ->
                    net.addLooseNeuron(neuron)
                }
            }

    fun <C: Chromosome<Neuron, NodeGene>> C.asGroup(
            options: NeuronGroup.() -> Unit = {  }
    ): (Network) -> Unit {
        return { net ->
            genes.map { gene ->
                gene.build(net).also { builder.productMap[gene] = it }
            }.let { net.addNeuronGroup(NeuronGroup(net, it).apply(options)) }
        }
    }

    @JvmName("unaryPlusSynapse")
    operator fun <C: Chromosome<Synapse, ConnectionGene>> C.unaryPlus() {
        addGene { gene, net ->
            gene.build(net, builder.productMap[gene.source], builder.productMap[gene.target])
                    .also { synapse -> net.addLooseSynapse(synapse) }
        }
    }


}

fun useNetwork() = NetworkBuilderProvider()