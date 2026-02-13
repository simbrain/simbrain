package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter

/**
 * Responds to a spike with a step response for a set number of iterations.
 */
class StepResponder(

    /**
     * Response duration (ms).
     */
    @UserParameter(label = "Response time", description = "Response duration (ms)", increment = 1.0, order = 1)
    var responseDuration: Int = 3

) : SpikeResponder() {

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val weightMatrix = connector as WeightMatrix
        val lastSpikeTimes = ((weightMatrix.source as NeuronArray).dataHolder as SpikingMatrixData).lastSpikeTimes
        for (i in 0 until connector.psrMatrix.ncol()) {
            for (j in 0 until connector.psrMatrix.nrow()) {
                if (lastSpikeTimes[i] + responseDuration * timeStep >= time && probabilisticSpikeCheck()) {
                    connector.psrMatrix[j, i] = connector.weights[j, i]
                } else {
                    connector.psrMatrix[j, i] = 0.0
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        if (synapse.source.lastSpikeTime + responseDuration * timeStep >= time && probabilisticSpikeCheck()) {
            synapse.rawPSR = synapse.strength
        } else {
            synapse.rawPSR = 0.0
        }
    }


    override fun copy(): StepResponder {
        val st = StepResponder()
        st.spikeProbability = spikeProbability
        st.responseDuration = responseDuration
        return st
    }

    override val description: String = "Step"

    override val name: String
        get() = "Step"
}
