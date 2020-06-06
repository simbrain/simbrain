package org.simbrain.custom_sims.builders.network

import org.simbrain.network.core.Synapse

class SynapseBuilder(source: NeuronBuilder, target: NeuronBuilder, val template: Synapse.() -> Unit):
        SynapseBuilderStub(source, target) {
    override fun buildProduct(context: (SynapseBuilder) -> Synapse) = context(this).apply(template)
}

open class SynapseBuilderStub(val source: NeuronBuilder, val target: NeuronBuilder) {
    operator fun invoke(template: Synapse.() -> Unit) = SynapseBuilder(source, target, template)
    open fun buildProduct(context: (SynapseBuilder) -> Synapse) = context(invoke {  })
}