package org.simbrain.custom_sims.builders.network

import org.simbrain.custom_sims.builders.SimComponentBuilder
import org.simbrain.network.core.Network
import org.simbrain.util.cartesianProduct

class NetworkBuilder : SimComponentBuilder<Network> {

    fun buildProduct(context: NetworkBuilder.() -> Network) = context()

    val neurons = ArrayList<NeuronBuilder>()
    val synapses = ArrayList<SynapseBuilderStub>()

    operator fun NeuronBuilder.unaryPlus() = apply {
        network = this@NetworkBuilder
        network.neurons.add(this)
    }

    operator fun SynapseBuilderStub.unaryPlus() = apply {
        this@NetworkBuilder.synapses.add(this)
    }

    infix fun NeuronBuilder.connectTo(other: NeuronBuilder) = SynapseBuilderStub(this, other)

    infix fun Collection<NeuronBuilder>.connectTo(other: NeuronBuilder): List<SynapseBuilderStub> {
        return this.map { source -> SynapseBuilderStub(source, other) }
    }

    infix fun NeuronBuilder.connectTo(other: Collection<NeuronBuilder>): List<SynapseBuilderStub> {
        return other.map { target -> SynapseBuilderStub(this, target) }
    }

    infix fun Collection<NeuronBuilder>.connectTo(other: Collection<NeuronBuilder>): List<SynapseBuilderStub> {
        // TODO: use connection strategy
        return (this cartesianProduct other)
                .map { (source, target) -> SynapseBuilderStub(source, target) }
    }


}