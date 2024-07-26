package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter

/**
 * Each time a spike occurs jump by a given value (the synaptic strength) and then decay.
 *
 * @author Zoë Tosi
 */
class ConvolvedJumpAndDecay() : SpikeResponder() {

    /**
     * Base line value.
     */
    @UserParameter(
        label = "Base-Line",
        description = "Decays to this value when no spikes occur.",
        increment = .1,
        order = 1
    )
    var baseLine = 0.0

    @UserParameter(
        label = "Time Constant",
        description = "Time constant of decay (ms). Roughly the time it takes to decay to\n" +
                "near-baseline. Larger time constants produce slower decay.",
        increment = .1,
        order = 2
    )
    var timeConstant = 3.0

    override fun copy(): ConvolvedJumpAndDecay {
        val jad = ConvolvedJumpAndDecay()
        jad.baseLine = baseLine
        return jad
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrow()) {
                for (j in 0 until wm.weightMatrix.ncol()) {
                    val psr = convolvedJumpAndDecay(
                        spikeData.spikes[j],
                        wm.psrMatrix[i, j],
                        wm.weightMatrix[i, j],
                        timeStep
                    )
                    wm.psrMatrix.set(i, j, psr)
                }
            }
        }
    }


    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        synapse.psr = convolvedJumpAndDecay(synapse.source.isSpike, synapse.psr, synapse.strength, timeStep)
    }

    fun convolvedJumpAndDecay(
        spiked: Boolean,
        psr: Double,
        jumpHeight: Double,
        timeStep: Double): Double {
        return if (spiked) {
            psr + jumpHeight
        } else {
            psr + timeStep * (baseLine - psr) / timeConstant
        }
    }

    override val description = "Convolved Jump and Decay"

    override val name = "Convolved Jump and Decay"
}