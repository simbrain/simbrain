/**
 * Settling, minus and plus phases, and XCAL learning for networks of [NeuronArray]s that use [LeabraRule],
 * connected by [WeightMatrix] projections. Simplified from emer/leabra (https://github.com/emer/leabra):
 *
 * - Net input to a layer sums its projections, each scaled by its relative strength and by the expected number
 *   of active senders, so that layers of different size and sparsity drive one another comparably.
 * - A trial resets activity, settles with the input clamped (minus phase, the network's own guess), then also clamps
 *   the target and settles further (plus phase, the outcome).
 * - Learning applies the XCAL check-mark function to sender-receiver coproducts, comparing the plus phase (with a
 *   little minus phase mixed in) to the minus phase. emer/leabra compares running averages of activity over short and
 *   medium time scales and adds a BCM-like Hebbian term; neither is included here.
 * - Learning is soft-bounded so weights stay in 0 to 1. The network computes its own net inputs rather than using
 *   the default Simbrain update, so layers driven by this trainer should be updated through [cycle].
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.LeabraMatrixData
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A Leabra projection. [relativeScale] sets this projection's share of the receiving layer's net input; feedback
 * projections are typically weaker than feedforward ones.
 */
class LeabraProjection(val weights: WeightMatrix, var relativeScale: Double = 1.0) {
    val source get() = weights.source as NeuronArray
    val target get() = weights.target as NeuronArray
}

class LeabraTrainer(
    val network: Network,
    val layers: List<NeuronArray>,
    val projections: List<LeabraProjection>,
    val inputLayer: NeuronArray,
    val outputLayer: NeuronArray
) {

    /**
     * emer/leabra uses 0.04 together with weight-change normalization and momentum, which are not implemented here.
     */
    var learningRate = 0.1

    /**
     * Fraction of minus phase activity mixed into plus phase activity for learning. Without it, a unit that was
     * wrongly active and is clamped off in the plus phase has a zero coproduct, which XCAL leaves unchanged.
     */
    var minusMix = 0.1

    var minusCycles = 75

    var plusCycles = 25

    /**
     * Expected fraction of active units in a layer, used to scale the net input it sends.
     */
    val expectedActivity = layers.associateWith { 0.15 }.toMutableMap()

    /**
     * XCAL activity level below which no learning occurs.
     */
    var xcalThreshold = 0.0001

    /**
     * XCAL reversal point, as a fraction of the threshold, below which weight change turns back toward zero.
     */
    var xcalReversal = 0.1


    /**
     * Run one settling cycle: compute net inputs from current activity, then update every unclamped layer.
     * [fireEvents] controls per-cycle GUI updates; the multi-cycle methods below always refresh layers when done.
     */
    fun cycle(fireEvents: Boolean = true) {
        val netInputs = layers.associateWith { DoubleArray(it.size) }
        val totalRelative = projections.groupBy { it.target }.mapValues { (_, ps) -> ps.sumOf { it.relativeScale } }
        for (p in projections) {
            if (p.target.isClamped) continue
            val source = p.source
            val expectedActive = max(1, (expectedActivity.getValue(source) * source.size).roundToInt())
            val scale = p.relativeScale / totalRelative.getValue(p.target) / expectedActive
            val drive = p.weights.weights.mv(source.activations.col(0))
            val net = netInputs.getValue(p.target)
            for (i in net.indices) net[i] += scale * drive[i]
        }
        with(network) {
            for (layer in layers) {
                if (layer.isClamped) continue
                val net = netInputs.getValue(layer)
                for (i in net.indices) layer.inputs[i, 0] = net[i]
                (layer.updateRule as LeabraRule).apply(layer, layer.dataHolder as LeabraMatrixData)
                layer.inputs.fill(0.0)
                if (fireEvents) layer.events.updated.fire()
            }
        }
    }

    fun resetActivity() {
        for (layer in layers) {
            if (layer.isClamped) continue
            (layer.updateRule as LeabraRule).reset(layer, layer.dataHolder as LeabraMatrixData)
            layer.events.updated.fire()
        }
    }

    /**
     * Clamp [input], unclamp the output, and reset the rest of the network, ready for a minus phase.
     */
    fun startTrial(input: DoubleArray) {
        clamp(inputLayer, input)
        outputLayer.isClamped = false
        resetActivity()
    }

    fun clamp(layer: NeuronArray, pattern: DoubleArray) {
        layer.isClamped = true
        layer.activations = pattern.toColumn()
    }

    /**
     * Run a minus phase on [input] and return the output's sum squared error against [target], if given.
     */
    fun test(input: DoubleArray, target: DoubleArray? = null, fireEvents: Boolean = true): Double {
        startTrial(input)
        repeat(minusCycles) { cycle(fireEvents) }
        fireAll()
        return target?.let { sse(outputLayer.activationArray, it) } ?: 0.0
    }

    /**
     * Run a minus and plus phase and update weights. Returns the minus phase sum squared error.
     */
    fun trainTrial(input: DoubleArray, target: DoubleArray, fireEvents: Boolean = false): Double {
        val error = test(input, target, fireEvents)
        val minus = snapshot()
        clamp(outputLayer, target)
        repeat(plusCycles) { cycle(fireEvents) }
        learn(minus, snapshot())
        fireAll()
        return error
    }

    /**
     * Update all weights given activity at the end of the minus and plus phases, as returned by [snapshot].
     */
    fun learn(minus: Map<NeuronArray, DoubleArray>, plus: Map<NeuronArray, DoubleArray>) {
        val mixedPlus = plus.mapValues { (layer, acts) ->
            val m = minus.getValue(layer)
            DoubleArray(acts.size) { (1 - minusMix) * acts[it] + minusMix * m[it] }
        }
        projections.forEach { learn(it, minus, mixedPlus) }
    }

    /**
     * Train once on each pattern in shuffled order. Returns mean minus phase sum squared error.
     */
    fun trainEpoch(inputs: List<DoubleArray>, targets: List<DoubleArray>): Double {
        val order = inputs.indices.shuffled()
        return order.sumOf { trainTrial(inputs[it], targets[it]) } / inputs.size
    }

    /**
     * Fraction of patterns whose minus phase output is off by more than 0.5 on some unit.
     */
    fun errorRate(inputs: List<DoubleArray>, targets: List<DoubleArray>): Double {
        val wrong = inputs.indices.count { i ->
            test(inputs[i], fireEvents = false)
            outputLayer.activationArray.zip(targets[i]).any { (a, t) -> abs(a - t) > 0.5 }
        }
        return wrong.toDouble() / inputs.size
    }

    private fun learn(p: LeabraProjection, minus: Map<NeuronArray, DoubleArray>, plus: Map<NeuronArray, DoubleArray>) {
        val xMinus = minus.getValue(p.source)
        val xPlus = plus.getValue(p.source)
        val yMinus = minus.getValue(p.target)
        val yPlus = plus.getValue(p.target)
        val w = p.weights.weights
        for (j in yPlus.indices) {
            for (i in xPlus.indices) {
                val dw = xcal(yPlus[j] * xPlus[i], yMinus[j] * xMinus[i])
                val wt = w[j, i]
                val bounded = if (dw > 0) dw * (1 - wt) else dw * wt
                w[j, i] = (wt + learningRate * bounded).coerceIn(0.0, 1.0)
            }
        }
        p.weights.events.updated.fire()
    }

    /**
     * The XCAL check-mark function: weight change is [coproduct] minus [floatingThreshold], except that it returns
     * to zero as [coproduct] approaches zero.
     */
    fun xcal(coproduct: Double, floatingThreshold: Double): Double = when {
        coproduct < xcalThreshold -> 0.0
        coproduct > floatingThreshold * xcalReversal -> coproduct - floatingThreshold
        else -> -coproduct * (1 - xcalReversal) / xcalReversal
    }

    /**
     * Current activations of every layer.
     */
    fun snapshot() = layers.associateWith { it.activationArray }

    fun randomizeWeights() = projections.forEach { it.weights.randomize(UniformRealDistribution(0.25, 0.75)) }

    private fun fireAll() = layers.forEach { it.events.updated.fire() }

    private fun DoubleArray.toColumn() = Matrix.column(this)

    private fun sse(a: DoubleArray, b: DoubleArray) = a.indices.sumOf { (a[it] - b[it]).let { d -> d * d } }
}

/**
 * A neuron array that uses [LeabraRule].
 */
fun leabraLayer(size: Int, label: String) = NeuronArray(size).apply {
    this.label = label
    updateRule = LeabraRule()
    activations.fill(0.0)
}

/**
 * Projections for every weight matrix in [network] whose source and target are both in [layers]. A matrix whose
 * source comes after its target in [layers] is a feedback projection and gets [feedbackScale]; the rest get 1.
 */
fun leabraProjections(network: Network, layers: List<NeuronArray>, feedbackScale: Double) =
    network.getModels<WeightMatrix>().filter { it.source in layers && it.target in layers }.map {
        val feedback = layers.indexOf(it.source as NeuronArray) > layers.indexOf(it.target as NeuronArray)
        LeabraProjection(it, if (feedback) feedbackScale else 1.0)
    }

/**
 * A projection from [source] to [target] with weights drawn uniformly from 0.25 to 0.75.
 */
fun leabraProjection(source: NeuronArray, target: NeuronArray, relativeScale: Double = 1.0): LeabraProjection {
    val weights = WeightMatrix(source, target)
    weights.randomize(UniformRealDistribution(0.25, 0.75))
    return LeabraProjection(weights, relativeScale)
}
