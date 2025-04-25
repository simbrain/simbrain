/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

private val WeightMatrix.sourceNeuronArray get() = source as NeuronArray
private val WeightMatrix.targetNeuronArray get() = target as NeuronArray

/**
 * Perform a "forward pass" through a list of weight matrices. Assumes they are all connected.
 */
context(Network)
@Deprecated("Migrating towards LinkedHashSet<Layer>.forwardPass")
fun List<WeightMatrix>.forwardPass(inputVector: Matrix, rowProbe: StructuredProbe? = null) {

    val rowProbe = rowProbe?.createMapProbe("ForwardPass")
    inputVector.validateSameShape(first().sourceNeuronArray.inputs)
    first().sourceNeuronArray.activations = inputVector
    rowProbe?.write("Input", inputVector)

    fun NeuronArray.updateWithoutClearingInputs() {
        updateRule.apply(this, dataHolder)
        events.updated.fire()
    }

    for (wm in this) {
        wm.target.inputs.fill(0.0)
        wm.target.accumulateInputs()
        (wm.target as NeuronArray).updateWithoutClearingInputs()
        rowProbe?.write(wm.displayName, wm.weights)

    }
}

/**
 * Backpropagate the provided errors through this weight matrix, and return the new error.
 */
 @Deprecated("Migrating towards LinkedHashSet<Layer>.accumulateBackprop")
fun WeightMatrix.updateWeights(errorSignal: Matrix, epsilon: Double = .1): Matrix {
    errorSignal.validateSameShape(target.activations)
    val weightDeltas = errorSignal.mm(source.activations.transpose())

    // Backpropagate the error signal through the weights to get a new error vector
    //  Prefer this to errorSignal.T.mm(wm).T because that requies an extra transpose
    val backropagatedErrors = weights.transpose().mm(errorSignal)

    // Update weights
    weights.add(weightDeltas.mul(epsilon))
    events.updated.fire()

    return backropagatedErrors
}

fun WeightMatrix.computeWeightDeltas(errorSignal: Matrix): Matrix {

    val sourceIsActivationSequenceProcessor = source is ActivationSequenceProcessor
    val targetIsActivationSequenceProcessor = target is ActivationSequenceProcessor

    // source is sequence and target is vector
    if (sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        // sum of outer products across rows of activation sequence
        return source.activations.rows.map { row ->
            errorSignal.mm(row)
        }.reduce { acc, matrix ->
            acc.add(matrix)
        }
    }

    // source and target are vectors
    if (!sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        return errorSignal.mm(source.activations.transpose())
    }

    // source and target are sequences
    if (sourceIsActivationSequenceProcessor && targetIsActivationSequenceProcessor) {
        return errorSignal.transpose().mm(source.activations)
    }

    throw IllegalArgumentException("Invalid source and target types: ${source::class.simpleName} and ${target::class.simpleName}")
}

fun SynapseGroup.computeWeightDeltas(errorSignal: Matrix): Matrix {
    return errorSignal.mm(source.activations.transpose())
}

fun WeightMatrix.backpropagateError(errorSignal: Matrix): Matrix {
    // Backpropagate the error signal through the weights to get a new error vector
    //println("Propagating errors through ${source.displayName} [${errorSignal.flatten().joinToString(", ") { it.format(2) }}]")
    return if (target is ActivationSequenceProcessor) {
        // sequence of errors * wm
        errorSignal.mm(weights)
    } else {
        // error vector * wm
        // Prefer this to errorSignal.T.mm(wm).T because that requires an extra transpose
        weights.transpose().mm(errorSignal)
    }
}

fun SynapseGroup.backpropagateError(errorSignal: Matrix): Matrix {
    return getWeightMatrix().transpose().mm(errorSignal)
}


/**
 * Change to bias is error vector times epsilon. Compute this and add it to biases.
 */
fun NeuronArray.updateBiases(error: Matrix, epsilon: Double = .1) {
    this.activations.validateSameShape(error)
    val biasDelta = error.clone().mul(epsilon)
    biases += biasDelta
    events.updated.fire()
}

sealed class BackpropLossFunction(
    val shortName: String,
    val description: String
) : EditableObject {

    object SSE : BackpropLossFunction("SSE", "Sum Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual sse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0)
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule
    }

    object MSE : BackpropLossFunction("MSE", "Mean Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual mse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0).div(actual.size().toDouble())
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule
    }

    object RMSE : BackpropLossFunction("RMSE", "Root Mean Squared Error") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual rmse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).div(actual.size() * scalarLoss(actual, target))
        }

        override fun canUse(layer: Layer) = layer.updateRule !is SoftmaxRule

    }

    object CrossEntropy : BackpropLossFunction("Cross Entropy", "Cross Entropy") {
        override fun scalarLoss(actual: Matrix, target: Matrix) = crossEntropy(actual, target)

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return actual.clone().sub(target).mul(-1.0) // assume softmax output
        }

        override fun canUse(layer: Layer) = layer.updateRule is SoftmaxRule
    }

    abstract fun scalarLoss(actual: Matrix, target: Matrix): Double

    abstract fun outputError(actual: Matrix, target: Matrix): Matrix

    abstract fun canUse(layer: Layer): Boolean

    override fun toString() = description

    fun validateLayer(layer: Layer) {
        if (!canUse(layer)) {
            throw IllegalArgumentException("Layer $layer cannot use loss function $this")
        }
    }
}

/**
 * Apply backprop algorithm to this list of matrices, for the provided input/target pair. Assumes weight matrices are
 * stored in a sequence from input to output layers
 */
context(Network)
@Deprecated("Migrating towards LinkedHashSet<Layer>.accumulateBackprop")
fun List<WeightMatrix>.applyBackprop(
    targetValues: Matrix,
    epsilon: Double = .1,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE,
    debug: (index: Int, errorSignal: List<Double>) -> Unit = { _, _ -> }
): Double {

    targetValues.validateSameShape(last().targetNeuronArray.activations)
    lossFunction.validateLayer(last().targetNeuronArray)

    val error = lossFunction.scalarLoss(last().targetNeuronArray.activations, targetValues)

    // printActivationsAndWeights()
    var errorSignal: Matrix = lossFunction.outputError(last().targetNeuronArray.activations, targetValues)

    this.reversed().forEachIndexed { index, wm ->
        debug(index, errorSignal.flatten().toList())
        (wm.targetNeuronArray.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.targetNeuronArray.inputs)?.let { deriv ->
            errorSignal.mul(deriv)
        }
        wm.targetNeuronArray.updateBiases(errorSignal, epsilon)
        errorSignal = wm.updateWeights(errorSignal, epsilon)
    }

    return error
}

context(Network)
@Deprecated("Migrating towards LinkedHashSet<Layer>.accumulateBackprop")
fun List<WeightMatrix>.accumulateBackprop(
    targetValues: Matrix,
    weightAccumulator: HashMap<WeightMatrix, Matrix>,
    biasesAccumulator: HashMap<NeuronArray, Matrix>,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE
): Double {

    targetValues.validateSameShape(last().targetNeuronArray.activations)
    lossFunction.validateLayer(last().targetNeuronArray)

    val error = lossFunction.scalarLoss(last().targetNeuronArray.activations, targetValues)

    // printActivationsAndWeights()
    var errorSignal: Matrix = lossFunction.outputError(last().targetNeuronArray.activations, targetValues)

    this.reversed().forEach { wm ->
        (wm.targetNeuronArray.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.targetNeuronArray.inputs)?.let {
            deriv -> errorSignal.mul(deriv)
        }
        biasesAccumulator.getOrPut(wm.targetNeuronArray) {
            Matrix(wm.targetNeuronArray.size, 1)
        }.add(errorSignal)
        val weightDeltas = wm.computeWeightDeltas(errorSignal)
        errorSignal = wm.backpropagateError(errorSignal)
        weightAccumulator.getOrPut(wm) {
            Matrix(wm.weights.nrow(), wm.weights.ncol())
        }.add(weightDeltas)
    }

    return error
}

context(Network)
fun WeightMatrixTree.forwardPass(inputVectors: List<Matrix>) {
    if (inputVectors.size != inputWeightLayers.size) throw IllegalArgumentException("Must provide same number of input vectors as input layers")
    inputVectors.zip(inputWeightLayers).forEach { (a, b) -> a.validateSameShape(b.sourceNeuronArray.inputs) }

    inputWeightLayers.zip(inputVectors).forEach { (wm, iv) -> wm.sourceNeuronArray.activations = iv }
    val allNeuronArrays = LinkedHashSet(tree.flatMap { it.map { it.target } })

    allNeuronArrays.forEach {
        it.accumulateInputs()
        it.update()
    }
}

/**
 * Apply backprop to a tree of weight matrices, beginning with the "output" weight matrix and backpropagating error
 * through incoming weight matrices.
 *
 * Weight matrices are updated one "weight layer" at a time. See [WeightMatrixTree] for more information.
 */
@Deprecated("Migrating towards LinkedHashSet<Layer>.accumulateBackprop")
fun WeightMatrixTree.applyBackprop(targetValues: Matrix, lossFunction: BackpropLossFunction = BackpropLossFunction.SSE, epsilon: Double = .0001): Double {

    targetValues.validateSameShape(outputWeightLayer.targetNeuronArray.activations)
    lossFunction.validateLayer(outputWeightLayer.targetNeuronArray)

    val error = lossFunction.scalarLoss(outputWeightLayer.targetNeuronArray.activations, targetValues)
    var errorVectors: Map<NeuronArray, Matrix> =
        mapOf(outputWeightLayer.targetNeuronArray to lossFunction.outputError(outputWeightLayer.targetNeuronArray.activations, targetValues))
    // TODO: Creating a map every iteration is a potential performance drain.
    tree.reversed().forEach { wms ->
        errorVectors = wms.associate { wm ->
            val tar = wm.targetNeuronArray
            val errorVector = errorVectors[tar]!!
            val deriv = (wm.targetNeuronArray.updateRule as DifferentiableUpdateRule).getDerivative(wm.targetNeuronArray.inputs)
            errorVector.mul(deriv)
            wm.targetNeuronArray.updateBiases(errorVector, epsilon)
            wm.sourceNeuronArray to wm.updateWeights(errorVector, epsilon)
        }

    }
    return error
}

/**
 * Computes the update order list for layers in a directed graph starting from the specified `start` layer
 * and ending at the specified `end` layer. This method traverses the incoming connectors of layers in a
 * reverse breadth-first manner to determine the proper order of updates.
 *
 * @param start The starting layer of the update process. The traversal will stop upon reaching this layer.
 * @param end The ending layer for the update process. The traversal begins from this layer and works backward.
 * @return A LinkedHashSet of layers representing the ordered list of layers to be updated.
 *         The order ensures layers are processed starting from the `start` layer to the `end` layer
 *         in accordance with their dependencies.
 */
fun computeOrderedUpdatePath(start:Layer, end: Layer): LinkedHashSet<Layer> {
    val visited = LinkedHashSet<Layer>()
    val queue = ArrayDeque<Layer>()
    queue.add(end)
    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()
        if (currentLayer in visited) {
            continue
        }
        visited.add(currentLayer)

        if (currentLayer == start) break

        for (neighbor in currentLayer.incomingConnectors) {
            if (neighbor.source !in visited) {
                queue.add(neighbor.source)
            }
        }
        (currentLayer as? AbstractNeuronCollection)?.incomingSgs?.forEach { neighbor ->
            if (neighbor.source !in visited) {
                queue.add(neighbor.source)
            }
        }
    }
    if (start !in visited) {
        throw IllegalArgumentException("No path found from start ($start) to end ($end)")
    }
    return LinkedHashSet(visited.reversed())
}

fun LinkedHashSet<Layer>.getAllOutgoingConnectors() = map { it.outgoingConnectors }
    .flatten()
    .filter { it.target in this }
    .toMutableList()

fun LinkedHashSet<Layer>.getAllOutgoingSynapseGroups() = filterIsInstance<AbstractNeuronCollection>()
    .map { it.outgoingSg }
    .flatten()
    .filter { it.target in this }
    .toMutableList()

/**
 *  Assumes LinkedHashSet has been placed in an appropriate "breadth-first" order by [computeOrderedUpdatePath].
 */
context(Network)
fun LinkedHashSet<Layer>.forwardPass(inputValues: List<Matrix>, inputLayers: List<Layer>, probe: StructuredProbe? = null) {

    val probeContext = probe?.createMapProbe("forwardPass")

    if (inputValues.size != inputLayers.size) throw IllegalArgumentException("Must provide same number of input vectors as input layers")
    inputValues.zip(inputLayers).forEach { (a, b) -> a.validateSameShape(b.activations) }

    fun NeuronArray.updateWithoutClearingInputs() {
        if (isClamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        events.updated.fire()
    }

    fun AbstractNeuronCollection.updateWithoutClearingInputs() {
        if (isClamped) {
            return
        }
        neuronList.forEach { neuron ->
            if (neuron.isSpike) {
                neuron.isSpike = false
            }
            if (!neuron.clamped) {
                neuron.updateRule.apply(neuron, neuron.dataHolder)
            }

        }
    }

    val allLayers = this
    inputLayers.zip(inputValues).forEach { (layer, value) ->
        layer.activations = value
    }
    probeContext?.createMapProbe("weightsInForwardPass")?.writeAll(
        flatMap {
            it.outgoingConnectors
        }.associate {
            it.displayName to (it as? WeightMatrix)?.weights?.clone()
        }
    )
    probeContext?.createMapProbe("activationsAfterApplyingTrainingInputs")?.writeAll(inputLayers.associate { it.displayName to it.activations.clone() })
    probeContext?.createMapProbe("biasesInForwardPass")?.writeAll(inputLayers.associate { it.displayName to it.biases.clone() })
    val layersContext = probeContext?.createMapProbe("layersInForwardPass")
    allLayers.forEach {
        val layerContext = layersContext?.createMapProbe(it.displayName)
        val inputsBeforeAccumulation = layerContext?.createMapProbe("inputsBeforeAccumulation")
        val inputsProbe = layerContext?.createMapProbe("inputs")
        val inputsAfterAccumulation = layerContext?.createMapProbe("inputsAfterAccumulation")
        it.clearInputs()
        when (it) {
            is NeuronArray -> {
                inputsBeforeAccumulation?.write(it.displayName, it.inputs.clone())
                it.accumulateInputs()
                inputsAfterAccumulation?.write(it.displayName, it.inputs.clone())
                it.updateWithoutClearingInputs()
            }
            is AbstractNeuronCollection -> {
                inputsBeforeAccumulation?.write(it.displayName, it.inputs.clone())
                if (it.incomingSgs.isNotEmpty()) {
                    it.neuronList.forEach { n -> n.accumulateInputs() }
                } else {
                    it.accumulateInputs()
                }
                inputsAfterAccumulation?.write(it.displayName, it.inputs.clone())
                it.updateWithoutClearingInputs()
            }
            else -> it.update()
        }
    }
    probeContext?.createMapProbe("afterUpdates")?.writeAll(allLayers.associate { it.displayName to it.activations.clone() })
}

/**
 * Main backprop implementation, which supports skip connections and accumulates weight and bias updates.
 *
 * Linked because it updates layers in a sequential order and Set because it only updates each layer once.
 * Assumes LinkedHashSet has been placed in an appropriate "breadth-first" order by [computeOrderedUpdatePath].
 *
 */
context(Network)
fun LinkedHashSet<Layer>.accumulateBackprop(
    targetValues: Matrix,
    outputLayer: Layer,
    weightAccumulator: HashMap<WeightMatrix, Matrix>,
    synapseGroupAccumulator: HashMap<SynapseGroup, Matrix>,
    biasesAccumulator: HashMap<Layer, Matrix>,
    rawMatrixAccumulator: HashMap<Matrix, Matrix>,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE,
    probe: StructuredProbe? = null,
): Double {

    val probeContext = probe?.createMapProbe("accumulateBackprop")

    val reversedLayers = drop(1).reversed()

    targetValues.validateSameShape(outputLayer.activations)
    lossFunction.validateLayer(outputLayer)

    val scalarError = lossFunction.scalarLoss(outputLayer.activations, targetValues)

    probeContext?.write("scalarError", scalarError)

    // printActivationsAndWeights()
    var errorSignal: Matrix = lossFunction.outputError(outputLayer.activations, targetValues)
    var signalSource: Layer = outputLayer

    probeContext?.write("errorSignal") { errorSignal.clone() }
    probeContext?.write("signalSource") { signalSource.displayName }

    val backpropLayersContext = probeContext?.createListProbe("backpropLayers")

    // Go through layers from output to input
    reversedLayers.forEach { layer ->

        val layerContext = backpropLayersContext?.createMapProbe(layer.displayName)

        // Process the error signal. Bias update for neuron array. Full backprop update for transformer block. Etc.
        errorSignal = layer.processError(errorSignal, signalSource, biasesAccumulator, rawMatrixAccumulator)

        layerContext?.write("processedErrorSignal") { errorSignal.clone() }

        // Process weight matrices feeding into the current layer.
        // For each one we:
        //  1. compute and accumulate a weight delta using source activations
        //  2. "backpropagate" the error signals by multiplying them by the weight matrix to get a new signal
        var accumulatedErrorSignal: Matrix? = null
        layer.incomingConnectors.forEach { connector ->
            val connectorContext = layerContext?.createMapProbe(connector.displayName)
            val wm = connector as WeightMatrix
            val weightDeltas = wm.computeWeightDeltas(errorSignal)
            connectorContext?.write("weightDeltas") { weightDeltas.clone() }
            weightAccumulator.getOrPut(wm) {
                Matrix(wm.weights.nrow(), wm.weights.ncol())
            }.add(weightDeltas)
            val currentConnectorErrorSignal = wm.backpropagateError(errorSignal)
            connectorContext?.write("currentConnectorErrorSignal") { currentConnectorErrorSignal.clone() }

            // The source of the weight matrix may be the same across multiple passes through this code,
            // supporting skip connections. In such cases, the error signals are summed
            accumulatedErrorSignal = accumulatedErrorSignal?.add(currentConnectorErrorSignal) ?: currentConnectorErrorSignal
            connectorContext?.write("accumulatedErrorSignal") { accumulatedErrorSignal?.clone() }
        }
        (layer as? AbstractNeuronCollection)?.incomingSgs?.forEach { sg ->
            val sgContext = layerContext?.createMapProbe(sg.displayName)
            val weightDeltas = sg.computeWeightDeltas(errorSignal)
            sgContext?.write("weightDeltas") { weightDeltas.clone() }
            synapseGroupAccumulator.getOrPut(sg) {
                Matrix(sg.target.size, sg.source.size)
            }.add(weightDeltas)
            val currentConnectorErrorSignal = sg.backpropagateError(errorSignal)
            sgContext?.write("currentConnectorErrorSignal") { currentConnectorErrorSignal.clone() }

            accumulatedErrorSignal = accumulatedErrorSignal?.add(currentConnectorErrorSignal) ?: currentConnectorErrorSignal
            sgContext?.write("accumulatedErrorSignal") { accumulatedErrorSignal.clone() }
        }
        // The new error signal which gets pushed further back until the first layer is reached
        accumulatedErrorSignal?.let { errorSignal = it } // null on the first layer
        signalSource = layer
    }

    return scalarError
}

/**
 * Returns a list or chain of connectors from input (start) to output (end).
 */
fun getConnectorChain(start: Layer, end: Layer): List<Connector> {

    // special case for recurrent connections from a layer to itself
    if (start === end) {
        return listOf(start.outgoingConnectors.first { it.target === end })
    }

    fun reconstructPath(start: Layer, end: Layer, path: Map<Layer, Connector>): List<Connector> {
        val result = mutableListOf<Connector>()
        var currentLayer: Layer? = end
        while (currentLayer != null && currentLayer != start) {
            result.add(path[currentLayer]!!)
            currentLayer = path[currentLayer]!!.source
        }

        result.reverse()
        return result
    }

    val visited = mutableSetOf<Layer>()
    val queue = ArrayDeque<Layer>()
    val path = mutableMapOf<Layer, Connector>()

    queue.add(start)

    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()

        if (currentLayer == end) {
            // We've found the end node, so we'll now reconstruct the path.
            return reconstructPath(start, end, path)
        }

        if (currentLayer in visited) {
            continue
        }

        visited.add(currentLayer)
        for (neighbor in currentLayer.outgoingConnectors) {
            if (neighbor.target !in visited) {
                queue.add(neighbor.target)
                path[neighbor.target] = neighbor
            }
        }
    }

    // If there's no path, return an empty list.
    return emptyList()
}

/**
 * A tree of weight matrices stored in the order they should be updated using backprop. The matrices are organized based
 * on the order in which they are updated during backpropagation. Stored as a list of lists of weight matrices.
 *
 * Supports multiple inputs but not skip connections.
 *
 * Example structure:
 * (
 *   (wm_out),                   # Output weight layer
 *   (wm_hidden_1),               # First hidden weight layer
 *   (wm_hidden_21, wm_hidden_22) # Second hidden weight layer with a branch
 *   ...
 * )
 */
@Deprecated("Planning to incorporate main use cases into LinkedHashSet.accumulateBackprop")
class WeightMatrixTree(start: List<Layer>, end: Layer) {
    val tree: List<List<WeightMatrix>>

    init {
        val validLayers = start.flatMap { getConnectorChain(it, end).filterIsInstance<WeightMatrix>() }.toMutableSet()
        tree = sequence {
            var frontier = listOf(end)
            while (validLayers.isNotEmpty()) {
                val weightMatrices = frontier.flatMap { it.incomingConnectors }
                    .filter { validLayers.contains(it) }
                    .filterIsInstance<WeightMatrix>()
                yield(weightMatrices)
                val layers = weightMatrices.map { it.source }
                frontier = layers
                validLayers.removeAll(weightMatrices.toSet())
            }
        }.toList().reversed()
    }

    val inputWeightLayers: List<WeightMatrix> = start
        .map { it.outgoingConnectors }
        .flatten()
        .filterIsInstance<WeightMatrix>()
        .toSet()
        .intersect(tree.flatten().toSet())
        .toList()
    val outputWeightLayer: WeightMatrix = tree.last().first()

}

/**
 * Print debugging info for a list of weight matrices.
 */
context(Network)
fun List<WeightMatrix>.printActivationsAndWeights(showWeights: Boolean = false) {
    println(first().source)
    for (wm in this) {
        wm.target.accumulateInputs()
        wm.target.update()
        println(wm)
        if (showWeights) {
            println(wm.weights)
        }
        println(wm.target)
    }

}

/**
 * Applies a repeating diagonal pattern to the matrix. The matrix is modified in-place.
 *
 * @return The matrix with the diagonal pattern applied.
 */
fun Matrix.applyDiagonalPattern(): Matrix {
    val smallerDimension = min(ncol(), nrow())
    this.setValuesInPlace { i, j ->
        if (i % smallerDimension == j % smallerDimension) 1.0 else 0.0
    }
    return this
}

/**
 * Split a dataset (inputs and targets) into training and testing subsets.
 */
fun splitDataSet(inputs: Matrix, targets: Matrix, splitRatio: Double, random: Random = Random(42L)): Pair<Pair<Matrix, Matrix>, Pair<Matrix, Matrix>> {
    require(inputs.nrow() == targets.nrow()) { "inputs nrow (${inputs.nrow()}) must equal targets nrow (${targets.nrow()})" }
    require(splitRatio in 0.0..1.0) { "splitRatio must be between 0.0 and 1.0" }

    val nrows = inputs.nrow()

    val rowIndices = (0 until nrows).shuffled(random)

    val trainRowCount = (nrows * splitRatio).toInt().coerceAtLeast(1)
    val testRowCount = (nrows - trainRowCount).coerceAtLeast(1)

    val trainRowIndices = rowIndices.take(trainRowCount)

    val testRowIndices = rowIndices.takeLast(testRowCount)

    return Pair(
        Pair(
            inputs.rows(*trainRowIndices.toIntArray()),
            targets.rows(*trainRowIndices.toIntArray())
        ),
        Pair(
            inputs.rows(*testRowIndices.toIntArray()),
            targets.rows(*testRowIndices.toIntArray())
        )
    )
}

/**
 * A hierarchical container for structured data capture, supporting both map-like and list-like organization.
 *
 *  Useful for tracing, logging, testing, and debugging complex data flows or computation trees.
 *
 * This abstract base class allows nesting of probe data via two concrete types: [MapProbe] for key-value mappings
 * and [ListProbe] for ordered collections. Each node can contain values or nested probe contexts.
 *
 * When passing a probe to a new context it's best to create a new probe from it (since you can't write to it directly).
 *
 * @property parent Optional parent context, enabling upward traversal or tree-like structures.
 */
sealed class StructuredProbe(var parent: StructuredProbe? = null): Iterable<Pair<Any, Any?>> {

    /**
     * Add key, value pairs to a probe. Basically if you want to give names to things you're probing.
     */
    class MapProbe(parent: StructuredProbe? = null) : StructuredProbe(parent) {
        val data = LinkedHashMap<String, Any?>()

        override fun iterator() = data.entries.map { it.toPair() }.iterator()

        override fun createMapProbe(key: String?): MapProbe {
            require(key != null) { "Key cannot be null for MapContext" }
            return MapProbe(parent = this).also { data[key] = it }
        }
        override fun createListProbe(key: String?): ListProbe {
            require(key != null) { "Key cannot be null for MapContext" }
            return ListProbe(parent = this).also { data[key] = it }
        }

        fun write(key: String, value: Any?) {
            data[key] = value
        }

        fun <K, V> writeAll(map: Map<K, V>, mappers: (K, V) -> Pair<String, Any?> = { k, v -> k.toString() to v }) {
            map.map { mappers(it.key, it.value) }.forEach { (key, value) ->
                write(key, value)
            }
        }

        fun write(key: String, valueProvider: () -> Any?) = write(key, valueProvider())

        override fun toTreeString(indentation: Int): String {
            return data.entries.joinToString("\n") { (key, value) ->
                " ".repeat(indentation) + key + ": \n" + ((value as? StructuredProbe)?.toTreeString(indentation + 2) ?: value.toString().indent(indentation + 2))
            }
        }
    }

    /**
     * Push probe items into a list. Useful mostly with foreach.
     */
    class ListProbe(parent: StructuredProbe? = null) : StructuredProbe(parent) {
        val data = ArrayList<Any?>()
        override fun iterator() = data.mapIndexed { index, value -> index to value }.iterator()
        override fun createMapProbe(key: String?) = MapProbe(parent = this).also { data.add(it) }
        override fun createListProbe(key: String?) = ListProbe(parent = this).also { data.add(it) }

        fun write(value: Any?) {
            data.add(value)
        }

        fun write(valueProvider: () -> Any?) = write(valueProvider())

        override fun toTreeString(indentation: Int): String {
            return data.mapIndexed { index, value ->
                " ".repeat(indentation) + index + ": \n" + ((value as? StructuredProbe)?.toTreeString(indentation + 2) ?: value.toString().indent(indentation + 2))
            }.joinToString("\n")
        }
    }

    abstract fun createMapProbe(key: String? = null): MapProbe

    abstract fun createListProbe(key: String? = null): ListProbe

    abstract fun toTreeString(indentation: Int = 0): String
}

fun diffProbes(a: StructuredProbe, b: StructuredProbe, allowMissing: Boolean = false, diffFunction: (Any, Any) -> Any? = { a, b ->
    when {
        a is Number && b is Number -> (a.toDouble() - b.toDouble()).let { if (abs(it) < 1e-6) true else it }
        a is Matrix && b is Matrix -> a.diff(b).let {
            when (it) {
                is MatrixDiffResult.InTolerance -> true
                is MatrixDiffResult.OutOfTolerance -> it.diff
                is MatrixDiffResult.DimensionsMismatch -> it.reason
            }
        }
        else -> a == b
    }
}): String {
    val result = StringBuilder()
    
    fun StructuredProbe.getPath(pathParts: MutableList<String> = mutableListOf()): List<String> {
        val parentPath = parent?.getPath(pathParts) ?: pathParts
        return parentPath
    }
    
    fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> value
            is Collection<*> -> "[${value.joinToString(", ") { it.toString() }}]"
            is Matrix -> (0..value.nrow() - 1).joinToString("\n") { i ->
                (0..value.ncol() - 1).joinToString(" ") { j ->
                    value[i, j].let {
                        (if (it >= 0) " " else "") + it.format(6)
                    }
                }
            }

            else -> value.toString()
        }.indent()
    }

    fun formatDiff(path: String, a: String, b: String, diff: String) = "$path:\n${a.indent(4)}\n  <->\n${b.indent(4)}\n  diff:\n${diff.indent(4)}\n"

    fun processNode(nodeA: Pair<Any, Any?>, nodeB: Pair<Any, Any?>?, path: List<String>) {
        val (keyA, valueA) = nodeA
        val currentPath = path + keyA.toString()
        
        // Handle leaf nodes
        if (valueA !is StructuredProbe && (nodeB == null || nodeB.second !is StructuredProbe)) {
            val pathStr = "/" + currentPath.joinToString("/")
            
            if (nodeB == null) {
                if (!allowMissing) {
                    result.append("${formatDiff(pathStr, formatValue(valueA), "missing", "missing")}\n")
                }
            } else {
                val (_, valueB) = nodeB
                val diff = diffFunction(valueA ?: "null", valueB ?: "null")
                val isDifferent = when (diff) {
                    is Boolean -> !diff
                    else -> true
                }
                
                if (isDifferent) {
                    val diffStr = when (diff) {
                        is Boolean -> diff.toString()
                        is Number -> diff.toString()
                        is Matrix -> formatValue(diff)
                        else -> "different"
                    }
                    
                    val formattedValueA = formatValue(valueA)
                    val formattedValueB = formatValue(valueB)

                    result.append(formatDiff(pathStr, formattedValueA, formattedValueB, diffStr))
                }
            }
            return
        }
        
        // Handle TrainerProbe nodes
        if (valueA is StructuredProbe && nodeB?.second is StructuredProbe) {
            val probeA = valueA
            val probeB = nodeB.second as StructuredProbe
            
            val aEntries = probeA.toList()
            val bMap = probeB.associate { it.first to it }
            
            // Compare each entry in the first probe
            for (entry in aEntries) {
                val matchingEntry = bMap[entry.first]
                processNode(entry, matchingEntry, currentPath)
            }
            
            // Check for entries that exist only in the second probe if not allowing missing
            if (!allowMissing) {
                val aKeys = aEntries.map { it.first }.toSet()
                val bOnlyEntries = probeB.filter { it.first !in aKeys }
                
                for (entry in bOnlyEntries) {
                    val (entryKey, entryValue) = entry
                    val pathStr = "/" + (currentPath + entryKey.toString()).joinToString("/")
                    result.append(formatDiff(pathStr, "missing", formatValue(entryValue), "missing"))
                }
            }
        }
    }
    
    // Start the recursion with the root nodes
    for (entryA in a) {
        val (keyA, _) = entryA
        val entryB = b.find { (keyB, _) -> keyB == keyA }
        processNode(entryA, entryB, emptyList())
    }
    
    // Check for entries in b that don't exist in a
    if (!allowMissing) {
        val aKeys = a.map { it.first }.toSet()
        val bOnlyEntries = b.filter { it.first !in aKeys }
        
        for (entryB in bOnlyEntries) {
            val (keyB, valueB) = entryB
            val pathStr = "/$keyB"
            result.append(formatDiff(pathStr, "missing", formatValue(valueB), "missing"))
        }
    }
    
    return result.toString().trimEnd()
}
