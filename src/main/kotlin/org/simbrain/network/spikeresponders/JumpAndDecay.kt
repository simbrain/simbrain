package org.simbrain.network.spikeresponders

import org.simbrain.network.core.*
import org.simbrain.network.learningrules.StaticSynapseRule
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

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder = JumpAndDecayMatrixData(cols)

    /**
     * With a zero baseline, fixed weights, and no spike probability, every synapse's response is its weight times a
     * trace kept per source neuron: the trace jumps to one on a spike (or by one, with [useConvolution]) and decays
     * toward zero otherwise. The weight matrix then sums responses from the spiking sources' weights. The trace
     * stands for the PSR matrix only once it has been started from an all-zero PSR matrix; after the full [apply]
     * runs, the PSR matrix is authoritative again. Editing a weight while its response decays rescales that response
     * at once, where the full PSR matrix keeps decaying from the old weight; the source's next spike makes them agree.
     */
    context(Network)
    override fun sourceFactors(connector: WeightMatrix): DoubleArray? {
        if (useSpikeProbability || baseLine != 0.0 || connector.learningRule !is StaticSynapseRule) return null
        val data = connector.spikeResponseData as? JumpAndDecayMatrixData ?: return null
        val source = connector.source as? NeuronArray ?: return null
        val spikes = (source.dataHolder as? SpikingMatrixData)?.spikes ?: return null
        val trace = data.trace
        if (trace.size != spikes.size) return null
        if (!data.traceValid) {
            val psrMatrix = connector.psrMatrix
            for (j in 0 until psrMatrix.ncol()) for (i in 0 until psrMatrix.nrow()) if (psrMatrix[i, j] != 0.0) return null
            trace.fill(0.0)
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
}
