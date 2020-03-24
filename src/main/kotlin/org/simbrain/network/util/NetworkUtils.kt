package org.simbrain.network.util

import org.simbrain.network.core.Neuron

var List<Neuron?>.activations
    get() = map { it?.activation ?: 0.0 }
    set(value) = (this zip value).forEach { (neuron, activation) ->
        if (neuron != null)
            if (neuron.isClamped) neuron.forceSetActivation(activation) else neuron.activation = activation
    }