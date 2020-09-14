package org.simbrain.network.util

import org.simbrain.network.core.Neuron

var List<Neuron?>.activations
    get() = map { it?.activation ?: 0.0 }
    set(values) = values.forEachIndexed { index, value ->
        this[index]?.let { neuron ->
            neuron.activation = value
        }
    }