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
        if (!data.traceStandsFor(connector)) {
            if (!startTrace(connector.weights, connector.psrMatrix, trace)) return null
            data.traceNowStandsFor(connector)
        }
        if (source.updateRule.isSpikingRule) {
            // A unit jump height: each response is its weight times this trace
            for (i in trace.indices) trace[i] = jumpAndDecay(spikes[i], trace[i], 1.0, timeStep)
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
        val wm = connector.let { if (it is WeightMatrix) it else return }
        val na = connector.source.let { if (it is NeuronArray) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            val psrMatrix = wm.materializePsrMatrix()
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val psr = jumpAndDecay(
                            spikeData.spikes[j] && spikeGetsThroughInOrder(),
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
            synapse.source.isSpike && spikeGetsThrough(synapse), synapse.rawPSR, synapse.strength, timeStep
        )
    }

    /**
     * The response after one step: [jumpHeight] (added to [psr] with [useConvolution]) if a spike [arrived], else
     * [psr] decayed toward the baseline. [arrived] is after any [spikeProbability] check, which callers make.
     */
    fun jumpAndDecay(arrived: Boolean, psr: Double, jumpHeight: Double, timeStep: Double): Double {
        return if (arrived) {
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
     * The [WeightMatrix.psrGeneration] at which [trace] last stood for the weight matrix's PSR matrix, or -1. A copy,
     * or a reloaded holder, starts over from the PSR matrix it is used with.
     */
    @Transient
    private var traceGeneration = -1L

    /** Whether [trace] stands for [weightMatrix]'s PSR matrix as it is now. */
    fun traceStandsFor(weightMatrix: WeightMatrix) = traceGeneration == weightMatrix.psrGeneration

    fun traceNowStandsFor(weightMatrix: WeightMatrix) {
        traceGeneration = weightMatrix.psrGeneration
    }

    override fun copy() = JumpAndDecayMatrixData(sources).also { trace.copyInto(it.trace) }

    override fun clear() {
        trace.fill(0.0)
        traceGeneration = -1
    }
}
