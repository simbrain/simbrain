package org.simbrain.network.updaterules.interfaces

import org.simbrain.util.UserParameter

/**
 * **Biased Neuron** is for neuron's with a bias, e.g. sigmoidal and linear
 * neurons.
 *
 *
 * TODO: Remove now that we have BiasedScalarData
 */
interface BiasedUpdateRule {
    @UserParameter(label = "Bias", description = "A fixed amount of input to this node.", increment = .1, order = 2)
    var bias: Double
}