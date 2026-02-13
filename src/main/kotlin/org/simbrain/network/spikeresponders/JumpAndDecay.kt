package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter

/**
 * When a spike occurs the jump to a max value (the synaptic strength) and then decay to baseline value..
 */
class JumpAndDecay : SpikeResponder() {

    /**
     * Base line value.
     */
    @UserParameter(
        label = "Base-Line",
        description = "The post-synaptic response value when no spike have occurred. Alternatively, the "
                + "post synaptic response to which decays to over time.",
        increment = .1,
        order = 2
    )
    var baseLine = 0.0

    @UserParameter(
        label = "Time Constant",
        description = "Time constant of decay (ms). Roughly the time it takes to decay to\n" +
                "near-baseline. Larger time constants produce slower decay.",
        increment = .1,
        minimumValue = 0.001,
        order = 3
    )
    var timeConstant = 3.0
        set(value) {
            field = value.coerceAtLeast(0.001)
        }

    @UserParameter(
        label = "Use Convolution",
        description = "If true the current spike response adds the psr from the previous iteration, which smoothes out the response.",
        order = 4
    )
    var useConvolution = false

    override fun copy(): JumpAndDecay {
        val jad = JumpAndDecay()
        jad.useConvolution = useConvolution
        jad.spikeProbability = spikeProbability
        jad.baseLine = baseLine
        jad.timeConstant = timeConstant
        return jad
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val psr = jumpAndDecay(
                            spikeData.spikes[j],
                            wm.psrMatrix[i, j],
                            wm.weights[i, j],
                            timeStep
                        )
                    wm.psrMatrix.set(i, j, psr)
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        synapse.rawPSR = jumpAndDecay(
            synapse.source.isSpike, synapse.rawPSR, synapse.strength, timeStep
        )
    }

    context(Network)
    fun jumpAndDecay(
        spiked: Boolean,
        psr: Double,
        jumpHeight: Double,
        timeStep: Double): Double {
        return if (spiked && probabilisticSpikeCheck()) {
            jumpHeight + (if (useConvolution) psr else 0.0)
        } else {
            psr + timeStep * ((baseLine - psr) / timeConstant)
        }
    }

    override val description: String = "Jump and Decay"

    override val name: String
        get() = "Jump and Decay"
}