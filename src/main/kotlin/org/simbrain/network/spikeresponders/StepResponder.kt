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

    /**
     * A step response is the weight while the source is within its response window and zero otherwise, so a weight
     * matrix can sum it from the weights of the responding sources. Spike probabilities draw per synapse, so they
     * take the full [apply].
     */
    context(Network)
    override fun sourceFactors(connector: WeightMatrix): DoubleArray? {
        if (useSpikeProbability) return null
        val lastSpikeTimes = ((connector.source as? NeuronArray)?.dataHolder as? SpikingMatrixData)?.lastSpikeTimes
            ?: return null
        return DoubleArray(lastSpikeTimes.size) { if (lastSpikeTimes[it] + responseDuration * timeStep >= time) 1.0 else 0.0 }
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val weightMatrix = connector as WeightMatrix
        val lastSpikeTimes = ((weightMatrix.source as NeuronArray).dataHolder as SpikingMatrixData).lastSpikeTimes
        val psrMatrix = connector.psrMatrix
        val weights = connector.weights
        for (i in 0 until psrMatrix.ncol()) {
            val responding = lastSpikeTimes[i] + responseDuration * timeStep >= time
            for (j in 0 until psrMatrix.nrow()) {
                if (responding && probabilisticSpikeCheck()) {
                    psrMatrix[j, i] = weights[j, i]
                } else {
                    psrMatrix[j, i] = 0.0
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
