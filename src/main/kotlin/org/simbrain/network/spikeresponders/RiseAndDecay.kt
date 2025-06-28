package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter
import smile.math.matrix.Matrix

/**
 * TODO
 */
class RiseAndDecay : SpikeResponder() {

    /**
     * The time constant of decay and recovery (ms).
     */
    @UserParameter(
        label = "Time constant",
        description = "Time constant for rising decay (ms). Roughly the time it takes to rise to max value then decay" +
                "to near-baseline. Larger time constants produce slower changes.",
        increment = .1,
        order = 2
    )
    var timeConstant = 3.0

    override fun copy(): RiseAndDecay {
        val rad = RiseAndDecay()
        rad.spikeProbability = spikeProbability
        rad.timeConstant = timeConstant
        return rad
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector as WeightMatrix
        val na = connector.source as NeuronArray
        val responseData = responderData as RiseAndDecayMatrixData
        val spikeData = na.dataHolder as SpikingMatrixData
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val (psr, recovery) = riseAndDecay(
                        spikeData.spikes[j],
                        wm.psrMatrix[i, j],
                        responseData.recoveryMatrix[i,j],
                        wm.weights[i, j],
                        timeStep
                    )
                    wm.psrMatrix.set(i, j, psr)
                    responseData.recoveryMatrix.set(i,j, recovery)
                }
            }
        }
    }

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return RiseAndDecayMatrixData(rows, cols)
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        val data = responderData as RiseAndDecayData
        val (psr, recovery) = riseAndDecay(
            synapse.source.isSpike,
            synapse.psr,
            data.recovery,
            synapse.strength,
            timeStep
        )
        synapse.psr = psr
        data.recovery = recovery
    }

    context(Network)
    fun riseAndDecay(spiked: Boolean,
                             psr: Double,
                             recovery: Double,
                             jumpHeight: Double,
                             timeStep: Double): Pair<Double, Double> {
        return Pair(
            (psr + ((timeStep / timeConstant) * (Math.E * jumpHeight * recovery * (1 - psr) - psr))),
            if (spiked && probabilisticSpikeCheck()) {
                1.0
            } else {
                recovery + timeStep / timeConstant * -recovery
            }
        )
    }

    override fun createResponderData(): ScalarDataHolder {
        return RiseAndDecayData()
    }

    override val description: String = "Rise and Decay"

    override val name: String
        get() = "Rise and Decay"
}


class RiseAndDecayData(
    @UserParameter(
        label = "Recovery"
    )
    var recovery: Double = 0.0,
) : ScalarDataHolder {
    override fun copy(): RiseAndDecayData {
        return RiseAndDecayData(recovery)
    }

    override fun clear() {
        recovery = 0.0
    }
}


class RiseAndDecayMatrixData(val rows: Int, val cols: Int) : MatrixDataHolder {
    @UserParameter("Recovery matrix")
    var recoveryMatrix = Matrix(rows, cols)
    override fun copy() = RiseAndDecayMatrixData(rows, cols).also {
        it.recoveryMatrix = recoveryMatrix.clone()
    }

    override fun clear() {
        recoveryMatrix.fill(0.0)
    }
}