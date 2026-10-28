package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter
import smile.math.matrix.Matrix
import kotlin.math.abs

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

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder = JumpAndDecayMatrixData(cols)

    /**
     * With a zero baseline, fixed weights, and no spike probability, every synapse's response is its weight times a
     * trace kept per source neuron: the trace jumps to one on a spike (or by one, with [useConvolution]) and decays
     * toward zero otherwise. The weight matrix then sums responses from the spiking sources' weights. After the full
     * [apply] runs, or the PSR matrix is replaced (as when it is edited), the PSR matrix is authoritative again until
     * the trace can be restarted from it (see [startTrace]). Editing a weight while its response decays rescales that response
     * at once, where the full PSR matrix keeps decaying from the old weight; the source's next spike makes them agree.
     */
    context(Network)
    override fun sourceFactors(connector: WeightMatrix): DoubleArray? {
        if (useSpikeProbability || baseLine != 0.0 || connector.isLearning) return null
        val data = connector.spikeResponseData as? JumpAndDecayMatrixData ?: return null
        val source = connector.source as? NeuronArray ?: return null
        val spikes = (source.dataHolder as? SpikingMatrixData)?.spikes ?: return null
        val trace = data.trace
        if (trace.size != spikes.size) return null
        if (!data.traceValid) {
            if (!startTrace(connector.weights, connector.psrMatrix, trace)) return null
            data.traceValid = true
        }
        if (source.updateRule.isSpikingRule) {
            for (i in trace.indices) {
                trace[i] = if (spikes[i]) {
                    1.0 + (if (useConvolution) trace[i] else 0.0)
                } else {
                    trace[i] + timeStep * ((0.0 - trace[i]) / timeConstant)
                }
            }
        }
        return trace.copyOf()
    }

    /**
     * Starts [trace] from [psrMatrix] if each column is its weights times one factor, the source's trace, and returns
     * whether it could. The full update leaves the PSR matrix that way after each source's spikes, and the decay keeps
     * it so, so the trace restarts on the step after the full update stops being needed. Differences within 1e-12 of
     * the largest weight are dropped, so that an edited response decaying toward zero, which it never quite
     * reaches, does not keep the full update running.
     */
    private fun startTrace(weights: Matrix, psrMatrix: Matrix, trace: DoubleArray): Boolean {
        var largestWeight = 0.0
        for (j in 0 until weights.ncol()) for (i in 0 until weights.nrow()) largestWeight = maxOf(largestWeight, abs(weights[i, j]))
        val tolerance = maxOf(1e-12 * largestWeight, java.lang.Double.MIN_NORMAL)
        for (j in 0 until weights.ncol()) {
            var reference = -1
            for (i in 0 until weights.nrow()) {
                if (abs(weights[i, j]) > (if (reference < 0) 0.0 else abs(weights[reference, j]))) reference = i
            }
            val factor = if (reference < 0) 0.0 else psrMatrix[reference, j] / weights[reference, j]
            for (i in 0 until weights.nrow()) {
                val expected = weights[i, j] * factor
                if (abs(psrMatrix[i, j] - expected) > tolerance + 1e-9 * abs(expected)) return false
            }
            trace[j] = factor
        }
        return true
    }

    context(Network)
    override fun apply(connector: Connector, responderData: MatrixDataHolder) {
        (responderData as? JumpAndDecayMatrixData)?.traceValid = false
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            val psrMatrix = wm.psrMatrix
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val psr = jumpAndDecay(
                            spikeData.spikes[j],
                            psrMatrix[i, j],
                            wm.weights[i, j],
                            timeStep
                        )
                    psrMatrix.set(i, j, psr)
                }
            }
        }
    }

    context(Network)
    override fun apply(synapse: Synapse, responderData: ScalarDataHolder) {
        synapse.rawPSR = jumpAndDecay(
            synapse.source.isSpike, synapse.rawPSR, synapse.strength, timeStep, synapse
        )
    }

    context(Network)
    fun jumpAndDecay(
        spiked: Boolean,
        psr: Double,
        jumpHeight: Double,
        timeStep: Double,
        synapse: Synapse? = null): Double {
        return if (spiked && probabilisticSpikeCheck(synapse)) {
            jumpHeight + (if (useConvolution) psr else 0.0)
        } else {
            psr + timeStep * ((baseLine - psr) / timeConstant)
        }
    }

    override val description: String = "Jump and Decay"

    override val name: String
        get() = "Jump and Decay"
}

/**
 * Per-source traces for a [JumpAndDecay] responder on a weight matrix; see [JumpAndDecay.sourceFactors].
 */
class JumpAndDecayMatrixData(val sources: Int) : MatrixDataHolder {

    val trace = DoubleArray(sources)

    /**
     * Whether [trace] currently stands for the weight matrix's PSR matrix.
     */
    var traceValid = false

    override fun copy() = JumpAndDecayMatrixData(sources).also {
        trace.copyInto(it.trace)
        it.traceValid = traceValid
    }

    override fun clear() {
        trace.fill(0.0)
        traceValid = false
    }

    override fun psrMatrixReplaced() {
        traceValid = false
    }
}
