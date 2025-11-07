package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.applyFunctionInPlace
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.setValuesInPlace
import org.simbrain.util.stats.distributions.NormalDistribution
import smile.math.matrix.Matrix
import kotlin.math.exp

/**
 * A spike responder with short term synaptic plasticity enabled.
 * Can model both short term depression (STD) and short term facilitation (STF)
 * Sometimes referred to as "UDF" for Use, Depression, and Facilitation
 *
 * Params for STD: A=1, U=0.45, D=750ms, F=50ms, responder decay = 20ms
 * Params for STF: U=0.15, F=750ms, and D=50ms.
 *
 * See http://www.scholarpedia.org/article/Short-term_synaptic_plasticity
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class ShortTermPlasticity : SpikeResponder() {

    @UserParameter(
        label = "Mean Use ",
        description = "Fraction (0 to 1) of available resources consumed to produce the post-synaptic current.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .15,
        order = 10
    )
    var U = 0.5

    /**
     * Depression constant.
     */
    @UserParameter(
        label = "Mean Depression ",
        description = "Time constant in ms for short term depression (STD). Higher values produce more STD",
        minimumValue = 0.0,
        increment = 10.0,
        order = 20
    )
    var D = 50.0

    /**
     * Facilitation constant.
     */
    @UserParameter(
        label = "Mean Facilitation ",
        description = "Time constant in ms. for short term facilitation (STF). Higher values produce more STF",
        minimumValue = 0.0,
        increment = 10.0,
        order = 30
    )
    var F = 750.0

    fun validSpikeResponderLocals() = listOf(
        JumpAndDecay::class.java,
    )

    var spikeResponderLocal by GuiEditable(
        JumpAndDecay().apply {
            useConvolution = true
            timeConstant = 20.0
        },
        typeMapProvider = ::validSpikeResponderLocals,
        label = "Spike Responder",
        description = "Short term plasticity sets the max response of this responder",
        showDetails = false,
        order = 50
    )

    /**
     * Does not actually copy this UDF object. Since UDF has values always
     * drawn from a distribution, it simply gives a new UDF object which
     * proceeds to draw its parameters from the same distributions.
     */
    override fun copy(): ShortTermPlasticity {
        val copy = ShortTermPlasticity()
        copy.spikeProbability = spikeProbability
        copy.U = U
        copy.D = D
        copy.F = F
        return copy
    }

    override val description: String
        get() {
            return "Short-term Plasticity"
        }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        val udfData = responderData as STPScalarData
        udfData.update(time, synapse.source.lastSpikeTime, U, D, F)
        val jumpHeight = udfData.R * synapse.strength * udfData.u
        val spiked = synapse.source.isSpike && probabilisticSpikeCheck()
        synapse.psr = when (val sr = spikeResponderLocal) {
            is JumpAndDecay -> sr.jumpAndDecay(spiked, synapse.psr, jumpHeight, timeStep)
            else -> throw IllegalStateException("STP can only be used with JumpAndDecay")
        }

    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        val wm = connector as WeightMatrix
        val na = connector.source as NeuronArray
        val stpData = responderData as STPMatrixData
        val spikeData = na.dataHolder as SpikingMatrixData
        if (na.updateRule.isSpikingRule) {
            stpData.update(time, spikeData.lastSpikeTimes, U, D, F)
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val jumpHeight = stpData.R[i, j] * wm.weights[i, j] * stpData.u[i, j]
                    val spiked = spikeData.spikes[j] && probabilisticSpikeCheck()
                    wm.psrMatrix.set(
                        i, j, when (val sr = spikeResponderLocal) {
                            is JumpAndDecay -> sr.jumpAndDecay(spiked, wm.psrMatrix[i, j], jumpHeight, timeStep)
                            else -> throw IllegalStateException("STP can only be used with JumpAndDecay")
                        })
                }
            }
        }
    }

    context(Network)
    private fun shortTermPlasticity(
        lastSpikeTime: Double,
        u: Double,
        R: Double,
    ): Pair<Double, Double> {
        val ISI = lastSpikeTime - time
        val newU = U + u * (1 - U) * exp(ISI / F)
        val newR = 1 + (R - u * R - 1) * exp(ISI / D)
        return Pair(newU, newR)
    }

    override fun createResponderData(): STPScalarData {
        return STPScalarData(U, 1.0)
    }

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return STPMatrixData(rows, cols)
    }

    override val name: String
        get() = "Short term plasticity"

    /**
     *
     * An intelligent randomization strategy for the responder’s parameters based on a source synapse.
     * Currently, there is no obvious way to integrate this into the GUI.
     *
     * Initializes this UDF object based on the synapse it governs. UDF draws
     * its values from different distributions based on the polarity of the
     * source and target neurons.
     *
     * @param s the synapse which is used to determine what polarities of
     * neurons the synapse connects and draw values based on that.
     */
    fun init(s: Synapse) {
        val minValue = 0.0000001
        
        when {
            s.source.polarity === Polarity.EXCITATORY
                    && s.target.polarity === Polarity.EXCITATORY -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble().coerceAtLeast(minValue)
                D = NormalDistribution(mean = 1100.0, standardDeviation = 550.0).sampleDouble().coerceAtLeast(minValue)
                F = NormalDistribution(mean = 50.0, standardDeviation = 25.0).sampleDouble().coerceAtLeast(minValue)
                (spikeResponderLocal as? JumpAndDecay)?.timeConstant = 3.0
            }
            s.source.polarity === Polarity.EXCITATORY
                    && s.target.polarity === Polarity.INHIBITORY -> {
                U = NormalDistribution(mean = 0.05, standardDeviation = 0.025).sampleDouble().coerceAtLeast(minValue)
                D = NormalDistribution(mean = 125.0, standardDeviation = 62.5).sampleDouble().coerceAtLeast(minValue)
                F = NormalDistribution(mean = 120.0, standardDeviation = 60.0).sampleDouble().coerceAtLeast(minValue)
                (spikeResponderLocal as? JumpAndDecay)?.timeConstant = 3.0
            }
            s.source.polarity === Polarity.INHIBITORY
                    && s.target.polarity === Polarity.EXCITATORY -> {
                U = NormalDistribution(mean = 0.25, standardDeviation = 0.125).sampleDouble().coerceAtLeast(minValue)
                D = NormalDistribution(mean = 700.0, standardDeviation = 350.0).sampleDouble().coerceAtLeast(minValue)
                F = NormalDistribution(mean = 20.0, standardDeviation = 10.0).sampleDouble().coerceAtLeast(minValue)
                (spikeResponderLocal as? JumpAndDecay)?.timeConstant = 6.0
            }
            s.source.polarity === Polarity.INHIBITORY
                    && s.target.polarity === Polarity.INHIBITORY -> {
                U = NormalDistribution(mean = 0.32, standardDeviation = 0.16).sampleDouble().coerceAtLeast(minValue)
                D = NormalDistribution(mean = 144.0, standardDeviation = 72.0).sampleDouble().coerceAtLeast(minValue)
                F = NormalDistribution(mean = 60.0, standardDeviation = 30.0).sampleDouble().coerceAtLeast(minValue)
                (spikeResponderLocal as? JumpAndDecay)?.timeConstant = 6.0
            }
            else -> {
                U = NormalDistribution(mean = 0.5, standardDeviation = 0.25).sampleDouble().coerceAtLeast(minValue)
                D = NormalDistribution(mean = 1100.0, standardDeviation = 550.0).sampleDouble().coerceAtLeast(minValue)
                F = NormalDistribution(mean = 50.0, standardDeviation = 25.0).sampleDouble().coerceAtLeast(minValue)
                (spikeResponderLocal as? JumpAndDecay)?.timeConstant = 3.0
            }
        }
    }
}

class STPScalarData(
    @UserParameter(label = "U", description = "Use/Facilitation variable", order = 1)
    var u: Double,
    @UserParameter(label = "R", description = "Depression variable", order = 2)
    var R: Double
) : ScalarDataHolder {
    override fun copy(): STPScalarData {
        return STPScalarData(u, R)
    }

    fun update(
        time: Double,
        lastSpikeTime: Double,
        U: Double,
        D: Double,
        F: Double,
    ) {
        val ISI = lastSpikeTime - time
        u = U + u * (1 - U) * exp(ISI / F)
        R = 1 + (R - u * R - 1) * exp(ISI / D)
    }

    override fun clear() {
        u = 0.0
        R = 0.0
    }
}

class STPMatrixData(val rows: Int, val cols: Int): MatrixDataHolder  {
    @UserParameter(label = "U", description = "Use/Facilitation variable", order = 1)
    var u = Matrix(rows, cols)
    @UserParameter(label = "R", description = "Depression variable", order = 2)
    var R = Matrix(rows, cols)

    fun update(
        time: Double,
        lastSpikeTime: DoubleArray,
        U: Double,
        D: Double,
        F: Double,
    ) {
        val ISIArray = lastSpikeTime.applyFunctionInPlace { it - time }
        u.setValuesInPlace { i, j ->
            val ISI = ISIArray[j]
            val u = this[i, j]
            U + u * (1 - U) * exp(ISI / F)
        }
        R.setValuesInPlace { i, j ->
            val ISI = ISIArray[j]
            val R = this[i, j]
            val u = u[i, j]
            1 + (R - u * R - 1) * exp(ISI / D)
        }
    }

    override fun copy() = STPMatrixData(rows, cols).also {
        it.u = u.clone()
        it.R = R.clone()
    }

    override fun clear() {
        u.fill(0.0)
        R.fill(0.0)
    }
}