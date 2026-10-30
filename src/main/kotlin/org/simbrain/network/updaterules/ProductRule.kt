package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.util.UserParameter

/**
 * **Product rule** units compute the product of the activations of incoming
 * units.  Used in "Long Short Term Memory" and "Sigma-Pi" networks.
 */
class ProductRule : LinearRule() {

    /**
     * Whether to use weights or not.
     */
    @UserParameter(
        label = "Use weights", description = "If false, activation is a product of incoming activations. "
                + "If true, activation is a product of incoming activation / weight products, or "
                + "(in the case of spiking neurons) post-synaptic-responses.", increment = .1, order = 0
    )
    var useWeights: Boolean = DEFAULT_USE_WEIGHTS

    override fun copy(): ProductRule {
        val pr = ProductRule()
        pr.useWeights = useWeights
        pr.addNoise = addNoise
        pr.upperBound = upperBound
        pr.lowerBound = lowerBound
        pr.noiseGenerator = noiseGenerator
        return pr
    }

    /** The product of the incoming activations, or of the post-synaptic responses with [useWeights]. */
    context(Network)
    override fun synapticInput(neuron: Neuron, data: EmptyScalarData, psrSum: Double): Double {
        // Special case of isolated neuron
        if (neuron.fanIn.size == 0) {
            return 0.0
        }
        var `val` = 1.0
        if (useWeights) {
            for (s in neuron.fanIn) {
                `val` *= s.rawPSR
            }
        } else {
            for (s in neuron.fanIn) {
                `val` *= s.source.activation
            }
        }
        return `val`
    }

    context(Network)
    override fun apply(neuron: Neuron, data: EmptyScalarData) {
        var `val` = neuron.input

        if (this.addNoise) {
            `val` += noiseGenerator.sampleDouble()
        }

        neuron.activation = `val`
    }

    override val name: String
        get() = "Product"

    companion object {
        /**
         * Whether to use weights by default.
         */
        private const val DEFAULT_USE_WEIGHTS = false
    }
}