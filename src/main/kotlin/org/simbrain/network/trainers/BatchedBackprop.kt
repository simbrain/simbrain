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
import org.simbrain.network.updaterules.interfaces.BatchActivation
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import org.simbrain.util.copyFrom
import org.simbrain.util.setColumn
import smile.math.blas.Transpose
import smile.math.matrix.Matrix

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
            activations[layer] = if (layer === inputLayer) inputs else (layer.updateRule as BatchActivation).activateColumns(z)
        }

        val output = activations.getValue(outputLayer)
        var lossSum = 0.0
        var accuracySum = 0.0
        var accuracyCount = 0
        val outputError = Matrix(outputLayer.size, batch)
        for (b in 0 until batch) {
            val actual = output.columnVector(b)
            val target = targets.columnVector(b)
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
            // Every layer feeds a later one, which the reversed order has already visited
            val error = errors.getValue(layer)
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
                layer.activations = inputs.columnVector(batch - 1)
            } else {
                layer.inputs.copyFrom(netInputs.getValue(layer).columnVector(batch - 1))
                layer.activations = activations.getValue(layer).columnVector(batch - 1)
            }
        }
        return Result(lossSum, accuracySum, accuracyCount)
    }

    /** Column [b] as a column vector. Not named `column`, which Smile's static Matrix.column(DoubleArray) would shadow. */
    private fun Matrix.columnVector(b: Int): Matrix = Matrix.column(DoubleArray(nrow()) { this[it, b] })

    private fun columnsOf(rowsOfData: List<DoubleArray>, rows: IntRange, size: Int): Matrix {
        val m = Matrix(size, rows.count())
        rows.forEachIndexed { b, row ->
            val values = rowsOfData[row]
            require(values.size == size) { "Row $row has ${values.size} values, but its layer has $size neurons" }
            m.setColumn(b, values)
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
         * [SupervisedModel] whose layers are plain [NeuronArray]s with a clamped input, no layer norm, and rules that
         * activate batches ([BatchActivation]: linear, sigmoidal, GELU, softmax) without noise, joined only by
         * [WeightMatrix]es without spike responders
         * between layers of the network, each running from an earlier layer to a later one in the network's layer
         * order. That order comes from a breadth-first search back from the output, so recurrent and self connections,
         * and some skip connections between hidden layers, are left to per-example training.
         */
        fun createOrNull(network: SupervisedNetwork): BatchedBackprop? {
            if (network !is BackpropNetwork && network !is SupervisedModel) return null
            val layers = network.layers.map { it as? NeuronArray ?: return null }
            if (layers.any { it.javaClass != NeuronArray::class.java || it.useLayerNorm }) return null
            val inputLayer = network.inputLayer as? NeuronArray ?: return null
            val outputLayer = network.outputLayer as? NeuronArray ?: return null
            if (!inputLayer.isClamped || inputLayer.incomingConnectors.isNotEmpty()) return null
            val position = layers.withIndex().associate { (index, layer) -> layer to index }
            for (layer in layers) {
                if (layer !== inputLayer) {
                    if (layer.isClamped) return null
                    val rule = layer.updateRule
                    if (rule !is BatchActivation || !rule.activatesBatches) return null
                    // Per-example training draws noise example by example through every layer, which a batched pass,
                    // going layer by layer over the batch, cannot reproduce
                    if (rule is NoisyUpdateRule && rule.addNoise) return null
                }
                if (layer.incomingFlattenConnectors.isNotEmpty()) return null
                for (connector in layer.incomingConnectors) {
                    if (connector.javaClass != WeightMatrix::class.java) return null
                    if ((connector as WeightMatrix).spikeResponder !is NonResponder) return null
                    val sourcePosition = position[connector.source] ?: return null
                    if (sourcePosition >= position.getValue(layer)) return null
                }
            }
            return BatchedBackprop(layers, inputLayer, outputLayer)
        }
    }
}
