package org.simbrain.custom_sims.builders.network

import org.simbrain.custom_sims.builders.AssignOnce
import org.simbrain.network.core.Neuron

class NeuronBuilder(val template: Neuron.() -> Unit) {

    var network by AssignOnce<NetworkBuilder>()

    fun buildProduct(context: (NetworkBuilder) -> Neuron)  = context(network).apply(template)

}