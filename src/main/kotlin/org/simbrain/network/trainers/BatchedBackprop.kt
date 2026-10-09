/**
 * Mini-batch backprop computed as matrix products: the batch's examples are the columns of one matrix, so each layer
 * costs one matrix-matrix product forward and two backward, instead of one matrix-vector product and a weight-sized
 * outer product per example.
 *
 * Used by [SupervisedTrainer.trainBatch] for plain feedforward stacks of [NeuronArray]s joined by [WeightMatrix]es (see
 * [BatchedBackprop.createOrNull]); everything else trains one example at a time through [forwardPass] and
 * [accumulateBackprop]. The two paths fill the same gradient accumulators with the same sums, so the optimizer step is
 * shared. Per-example semantics are reproduced exactly, including those of the update rules' layer updates and the
 * per-example loss functions; only the summation order of floating point products differs.
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.spikeresponders.NonResponder
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.GELU
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.util.applyFunction
import org.simbrain.util.copyFrom
import smile.math.blas.Transpose
import smile.math.matrix.Matrix
import kotlin.math.exp

class BatchedBackprop private constructor(
    private val layers: List<NeuronArray>,
    private val inputLayer: NeuronArray,
    private val outputLayer: NeuronArray,
) {

    /**
     * Totals over a batch, in the units [SupervisedTrainer.trainBatch] reports.
     */
    class Result(val lossSum: Double, val accuracySum: Double, val accuracyCount: Int)

    /**
     * Forward and backward pass over the examples in [rows] of [data], adding summed weight and bias deltas to the
     * accumulators exactly as one [accumulateBackprop] call per example would. Afterwards each layer shows the batch's
     * last example, as it would after per-example training.
     */
    context(Network)
    fun run(
        data: TrainingDataset,
        rows: IntRange,
        lossFunction: BackpropLossFunction,
        computeAccuracy: Boolean,
        weightAccumulator: HashMap<WeightMatrix, Matrix>,
        biasesAccumulator: HashMap<Layer, Matrix>,
    ): Result {
        lossFunction.validateLayer(outputLayer)
        val batch = rows.count()
        val inputs = columnsOf(data.inputs, rows, inputLayer.size)
        val targets = columnsOf(data.targets, rows, outputLayer.size)

        val netInputs = HashMap<NeuronArray, Matrix>()
        val activations = HashMap<NeuronArray, Matrix>()
        for (layer in layers) {
            // Every layer accumulates inputs and biases; a clamped layer (the input) keeps its activations
            val z = broadcastColumn(layer.biases, batch)
            // Smile's in-place product writes into its receiver: C.mm(tA, A, tB, B, alpha, beta) sets C = alpha*A*B + beta*C
            for (connector in layer.incomingConnectors) {
                val wm = connector as WeightMatrix
                z.mm(Transpose.NO_TRANSPOSE, wm.weights, Transpose.NO_TRANSPOSE, activations.getValue(wm.source as NeuronArray), 1.0, 1.0)
            }
            netInputs[layer] = z
            activations[layer] = if (layer === inputLayer) inputs else activate(layer, z)
        }

        val output = activations.getValue(outputLayer)
        var lossSum = 0.0
        var accuracySum = 0.0
        var accuracyCount = 0
        val outputError = Matrix(outputLayer.size, batch)
        for (b in 0 until batch) {
            val actual = output.column(b)
            val target = targets.column(b)
            lossSum += lossFunction.scalarLoss(actual, target)
            if (computeAccuracy) {
                classificationAccuracy(actual, target)?.let {
                    accuracySum += it
                    accuracyCount++
                }
            }
            val error = lossFunction.outputError(actual, target)
            for (i in 0 until outputLayer.size) outputError[i, b] = error[i, 0]
        }

        val errors = HashMap<NeuronArray, Matrix>()
        errors[outputLayer] = outputError
        for (layer in layers.asReversed()) {
            val error = errors[layer] ?: outputError
            (layer.updateRule as? DifferentiableUpdateRule)?.let { error.mul(it.getDerivative(netInputs.getValue(layer))) }
            biasesAccumulator.getOrPut(layer) { Matrix(layer.size, 1) }.add(Matrix.column(error.rowSums()))
            for (connector in layer.incomingConnectors) {
                val wm = connector as WeightMatrix
                val source = wm.source as NeuronArray
                // Summed over the batch: error * source activations^T
                val delta = weightAccumulator.getOrPut(wm) { Matrix(wm.weights.nrow(), wm.weights.ncol()) }
                delta.mm(Transpose.NO_TRANSPOSE, error, Transpose.TRANSPOSE, activations.getValue(source), 1.0, 1.0)
                val sourceError = errors.getOrPut(source) { Matrix(source.size, batch) }
                sourceError.mm(Transpose.TRANSPOSE, wm.weights, Transpose.NO_TRANSPOSE, error, 1.0, 1.0)
            }
        }

        for (layer in layers) {
            if (layer === inputLayer) {
                layer.activations = inputs.column(batch - 1)
            } else {
                layer.inputs.copyFrom(netInputs.getValue(layer).column(batch - 1))
                layer.activations = activations.getValue(layer).column(batch - 1)
            }
        }
        return Result(lossSum, accuracySum, accuracyCount)
    }

    /**
     * Applies a layer's update rule to every column of [z], as the rule's own layer update does to a single column.
     */
    private fun activate(layer: NeuronArray, z: Matrix): Matrix = when (val rule = layer.updateRule) {
        is LinearRule -> z.applyFunction(rule::linearRule)
        is GELU -> z.applyFunction(rule::gelu)
        is SigmoidalRule -> {
            val weighted = if (rule.addNoise) z.clone().also { m ->
                for (j in 0 until m.ncol()) for (i in 0 until m.nrow()) m.add(i, j, rule.noiseGenerator.sampleDouble())
            } else z
            rule.type.valueOf(weighted, rule.upperBound, rule.lowerBound, rule.slope)
        }
        is SoftmaxRule -> softmaxColumns(z, rule.temperature)
        else -> error("Unsupported rule ${rule::class.simpleName}")
    }

    /**
     * Matches [SoftmaxRule]'s layer update column by column, including its uniform fallback when every exponential
     * underflows.
     */
    private fun softmaxColumns(z: Matrix, temperature: Double): Matrix {
        val result = Matrix(z.nrow(), z.ncol())
        val exponentials = DoubleArray(z.nrow())
        for (b in 0 until z.ncol()) {
            var max = Double.NEGATIVE_INFINITY
            for (i in 0 until z.nrow()) max = maxOf(max, z[i, b])
            var total = 0.0
            for (i in 0 until z.nrow()) {
                exponentials[i] = exp((z[i, b] - max) / temperature)
                total += exponentials[i]
            }
            for (i in 0 until z.nrow()) {
                result[i, b] = if (total < 1e-6) 1.0 / z.nrow() else exponentials[i] / total
            }
        }
        return result
    }

    private fun Matrix.column(b: Int): Matrix = Matrix.column(DoubleArray(nrow()) { this[it, b] })

    private fun columnsOf(rowsOfData: List<List<Double>>, rows: IntRange, size: Int): Matrix {
        val m = Matrix(size, rows.count())
        rows.forEachIndexed { b, row ->
            val values = rowsOfData[row]
            for (i in 0 until size) m[i, b] = values[i]
        }
        return m
    }

    private fun broadcastColumn(column: Matrix, batch: Int): Matrix {
        val m = Matrix(column.nrow(), batch)
        for (b in 0 until batch) for (i in 0 until column.nrow()) m[i, b] = column[i, 0]
        return m
    }

    companion object {

        /**
         * A batched plan for [network], or null if it needs per-example training. Supported: a [BackpropNetwork] or
         * [SupervisedModel] whose layers are plain [NeuronArray]s with a clamped input, no layer norm, and linear,
         * sigmoidal, GELU, or softmax rules, joined only by [WeightMatrix]es without spike responders between layers
         * of the network.
         */
        fun createOrNull(network: SupervisedNetwork): BatchedBackprop? {
            if (network !is BackpropNetwork && network !is SupervisedModel) return null
            val layers = network.layers.map { it as? NeuronArray ?: return null }
            if (layers.any { it.javaClass != NeuronArray::class.java || it.useLayerNorm }) return null
            val inputLayer = network.inputLayer as? NeuronArray ?: return null
            val outputLayer = network.outputLayer as? NeuronArray ?: return null
            if (!inputLayer.isClamped || inputLayer.incomingConnectors.isNotEmpty()) return null
            val layerSet = layers.toSet()
            for (layer in layers) {
                if (layer !== inputLayer) {
                    if (layer.isClamped) return null
                    val rule = layer.updateRule
                    val supported = rule.javaClass == LinearRule::class.java || rule.javaClass == SigmoidalRule::class.java ||
                            rule.javaClass == GELU::class.java || rule.javaClass == SoftmaxRule::class.java
                    if (!supported) return null
                }
                if (layer.incomingFlattenConnectors.isNotEmpty()) return null
                for (connector in layer.incomingConnectors) {
                    if (connector.javaClass != WeightMatrix::class.java) return null
                    if ((connector as WeightMatrix).spikeResponder !is NonResponder) return null
                    if (connector.source !in layerSet) return null
                }
            }
            return BatchedBackprop(layers, inputLayer, outputLayer)
        }
    }
}
