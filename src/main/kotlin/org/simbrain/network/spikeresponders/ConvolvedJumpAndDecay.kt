package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Synapse
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder
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

    override fun deepCopy(): ConvolvedJumpAndDecay {
        val jad = ConvolvedJumpAndDecay()
        jad.baseLine = baseLine
        return jad
    }

    override fun apply(conn: Connector, responderData: MatrixDataHolder) {
        val wm = conn.let { if (it is WeightMatrix) it else return }
        val na = conn.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrows()) {
                for (j in 0 until wm.weightMatrix.ncols()) {
                    val psr = convolvedJumpAndDecay(
                        spikeData.spikes[j],
                        wm.psrMatrix[i, j],
                        wm.weightMatrix[i, j],
                        na.network.timeStep
                    )
                    wm.psrMatrix.set(i, j, psr)
                }
            }
        }
    }


    override fun apply(s: Synapse, responderData: ScalarDataHolder) {
        s.psr = convolvedJumpAndDecay(s.source.isSpike, s.psr, s.strength, s.network.timeStep)
    }

    fun convolvedJumpAndDecay(
        spiked: Boolean,
        initPsr: Double,
        jump: Double,
        timeStep: Double): Double {
        var psr = initPsr
        if (spiked) {
            psr += jump
        } else {
            psr += timeStep * (baseLine - psr) / timeConstant
        }
        return psr
    }

    override fun getDescription(): String {
        return "Convolved Jump and Decay"
    }

    override val name: String
        get() = "Convolved Jump and Decay"
}