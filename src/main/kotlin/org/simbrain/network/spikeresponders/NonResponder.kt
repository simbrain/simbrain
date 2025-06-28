package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.util.ScalarDataHolder

/**
 * A "null" spike responder which produces "connectionist" dynamics where the post-synaptic response is the weight
 * times the source activation. See [Synapse.updatePSR] and [WeightMatrix.getOutput]
 */
class NonResponder : SpikeResponder() {

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        // No implementation. The responder is bypassed.
    }

    override fun copy(): SpikeResponder {
        return NonResponder()
    }

    override val description: String = "None (No spike response)"

    override val name: String
        get() = "None"
}