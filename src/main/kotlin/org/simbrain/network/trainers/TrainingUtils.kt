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
import smile.math.matrix.Matrix
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.associate
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.filterIsInstance
import kotlin.collections.first
import kotlin.collections.flatMap
import kotlin.collections.flatten
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.getOrPut
import kotlin.collections.intersect
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.reverse
import kotlin.collections.reversed
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMutableSet
import kotlin.collections.toSet
import kotlin.collections.zip

private val WeightMatrix.sourceNeuronArray get() = source as NeuronArray
private val WeightMatrix.targetNeuronArray get() = target as NeuronArray

/**
 * Perform a "forward pass" through a list of weight matrices. Assumes they are all connected.
 */
context(Network)
fun List<WeightMatrix>.forwardPass(inputVector: Matrix) {
    inputVector.validateSameShape(first().sourceNeuronArray.inputs)
    first().sourceNeuronArray.activations = inputVector

    fun NeuronArray.updateWithoutClearingInputs() {
        updateRule.apply(this, dataHolder)
        events.updated.fire()
    }

    for (wm in this) {
        wm.target.inputs.mul(0.0)
        wm.target.accumulateInputs()
        (wm.target as NeuronArray).updateWithoutClearingInputs()
    }
}

/**
 * Backpropagate the provided errors through this weight matrix, and return the new error.
 */
 @Deprecated("Migrating towards LinkedHashSet<Layer>.accumulateBackprop")
fun WeightMatrix.updateWeights(layerError: Matrix, epsilon: Double = .1): Matrix {
    layerError.validateSameShape(target.activations)
    val weightDeltas = layerError.mm(source.activations.transpose())

    // Backpropagate the layer error through the weights to get a new error vector
    //  Prefer this to layerError.T.mm(wm).T because that requies an extra transpose
    val backropagatedErrors = weightMatrix.transpose().mm(layerError)

    // Update weights
    weightMatrix.add(weightDeltas.mul(epsilon))
    events.updated.fire()

    return backropagatedErrors
}

fun WeightMatrix.computeWeightDeltas(layerError: Matrix): Matrix {

    val sourceIsActivationSequenceProcessor = source is ActivationSequenceProcessor
    val targetIsActivationSequenceProcessor = target is ActivationSequenceProcessor

    // source is batch and target is vector
    if (sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        return layerError.mm(source.activations.row(source.activations.nrow() - 1).toMatrix().transpose())
    }

    // source and target are vectors
    if (!sourceIsActivationSequenceProcessor && !targetIsActivationSequenceProcessor) {
        return layerError.mm(source.activations.transpose())
    }

    // source and target are batches
    if (sourceIsActivationSequenceProcessor && targetIsActivationSequenceProcessor) {
        return layerError.transpose().mm(source.activations)
    }

    throw IllegalArgumentException("Invalid source and target types: ${source::class.simpleName} and ${target::class.simpleName}")
}

fun WeightMatrix.backpropagateError(layerError: Matrix): Matrix {
    // Backpropagate the layer error through the weights to get a new error vector
    //println("Propagating errors through ${source.displayName} [${layerError.flatten().joinToString(", ") { it.format(2) }}]")
    return if (target is ActivationSequenceProcessor) {
        // batch of errors * wm
        layerError.mm(weightMatrix)
    } else {
        // error vector * wm
        // Prefer this to layerError.T.mm(wm).T because that requies an extra transpose
        weightMatrix.transpose().mm(layerError)
    }
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

enum class BackpropLossFunction {

    SSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual sse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0)
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "SSE"

        override val description = "Sum Squared Error"
    },

    MSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual mse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0).div(actual.size().toDouble())
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "MSE"

        override val description = "Mean Squared Error"

    },

    RMSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual rmse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).div(actual.size() * scalarLoss(actual, target))
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "RMSE"

        override val description = "Root Mean Squared Error"

    },

    CrossEntropy {
        override fun scalarLoss(actual: Matrix, target: Matrix) = crossEntropy(actual, target)

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return actual.clone().sub(target).mul(-1.0) // assume softmax output
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule is SoftmaxRule

        override val shortName = "CrossEntropy"

        override val description = "Cross Entropy"

    };


    abstract fun scalarLoss(actual: Matrix, target: Matrix): Double

    abstract fun outputError(actual: Matrix, target: Matrix): Matrix

    abstract fun canUse(layer: NeuronArray): Boolean

    abstract val shortName: String

    abstract val description: String

    override fun toString() = description

    fun validateLayer(layer: NeuronArray) {
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
    debug: (index: Int, layerError: List<Double>) -> Unit = { _, _ -> }
): Double {

    targetValues.validateSameShape(last().targetNeuronArray.activations)
    lossFunction.validateLayer(last().targetNeuronArray)

    val error = lossFunction.scalarLoss(last().targetNeuronArray.activations, targetValues)

    // printActivationsAndWeights()
    var layerError: Matrix = lossFunction.outputError(last().targetNeuronArray.activations, targetValues)

    this.reversed().forEachIndexed { index, wm ->
        debug(index, layerError.flatten().toList())
        (wm.targetNeuronArray.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.targetNeuronArray.inputs)?.let { deriv ->
            layerError.mul(deriv)
        }
        wm.targetNeuronArray.updateBiases(layerError, epsilon)
        layerError = wm.updateWeights(layerError, epsilon)
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
    var layerError: Matrix = lossFunction.outputError(last().targetNeuronArray.activations, targetValues)

    this.reversed().forEach { wm ->
        (wm.targetNeuronArray.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.targetNeuronArray.inputs)?.let {
            deriv -> layerError.mul(deriv)
        }
        biasesAccumulator.getOrPut(wm.targetNeuronArray) {
            Matrix(wm.targetNeuronArray.size, 1)
        }.add(layerError)
        val weightDeltas = wm.computeWeightDeltas(layerError)
        layerError = wm.backpropagateError(layerError)
        weightAccumulator.getOrPut(wm) {
            Matrix(wm.weightMatrix.nrow(), wm.weightMatrix.ncol())
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
 * Apply backprop to a tree of weight matrices, beginning with the “output” weight matrix and backpropagating error
 * through incoming weight matrices.
 *
 * Weight matrices are updated one “weight layer” at a time. See [WeightMatrixTree] for more information.
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
 * Breadth-first search starting from the output layer.
 */
fun computeUpdateOrderList(end: Layer): LinkedHashSet<Layer> {
    val visited = LinkedHashSet<Layer>()
    val queue = ArrayDeque<Layer>()
    queue.add(end)
    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()
        if (currentLayer in visited) {
            continue
        }
        visited.add(currentLayer)
        for (neighbor in currentLayer.incomingConnectors) {
            if (neighbor.source !in visited) {
                queue.add(neighbor.source)
            }
        }
    }
    return LinkedHashSet(visited.reversed())
}

/**
 *  Assumes LinkedHashSet has been placed in an appropriate "breadth-first" order by [computeUpdateOrderList].
 */
context(Network)
fun LinkedHashSet<Layer>.forwardPass(inputValues: List<Matrix>, inputLayers: List<Layer>) {

    if (inputValues.size != inputLayers.size) throw IllegalArgumentException("Must provide same number of input vectors as input layers")
    inputValues.zip(inputLayers).forEach { (a, b) -> a.validateSameShape(b.activations) }

    val allLayers = this
    inputLayers.zip(inputValues).forEach { (layer, value) ->
        (layer as? ArrayLayer)?.activations = value
    }
    allLayers.forEach {
        it.accumulateInputs()
        it.update()
    }
}

/**
 * Main backprop implementation, which supports skip connections and accumulates weight and bias updates.
 *
 * Linked because it updates layers in a sequential order and Set because it only updates each layer once.
 * Assumes LinkedHashSet has been placed in an appropriate "breadth-first" order by [computeUpdateOrderList].
 *
 */
context(Network)
fun LinkedHashSet<Layer>.accumulateBackprop(
    targetValues: Matrix,
    outputLayer: Layer,
    weightAccumulator: HashMap<WeightMatrix, Matrix>,
    biasesAccumulator: HashMap<ArrayLayer, Matrix>,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE
): Double {

    val reversedLayers = reversed()

    targetValues.validateSameShape(outputLayer.activations)
    lossFunction.validateLayer(outputLayer as NeuronArray)

    val scalarError = lossFunction.scalarLoss(outputLayer.activations, targetValues)

    // printActivationsAndWeights()
    var layerError: Matrix = lossFunction.outputError(outputLayer.activations, targetValues)

    // Go through layers from output to input
    reversedLayers.forEach { layer ->

        // Apply derivative to layer error and accumulate bias updates
        when (layer) {
            is NeuronArray -> {
                // Element-wise product of the layer error with the derivative applied to the weighted inputs
                (layer.updateRule as? DifferentiableUpdateRule)?.getDerivative(layer.inputs)?.let {
                        deriv -> layerError.mul(deriv)
                }
                // The scaled layer error is used for bias update
                biasesAccumulator.getOrPut(layer) {
                    Matrix(layer.size, 1)
                }.add(layerError)
            }
            is ActivationSequenceProcessor -> {
                // temporary until we determine how to implement this
                val newError = Matrix(layer.activations.nrow(), layer.activations.ncol())
                layerError = newError
            }
        }

        // Process weight matrices feeding into the current layer.
        // For each one we:
        //  1. compute and accumulate a weight delta using source activations
        //  2. "backpropagate" the errors by multiplying them by the weight matrix to get a new error
        var accumulatedLayerError: Matrix? = null
        layer.incomingConnectors.forEach { connector ->
            val wm = connector as WeightMatrix
            val weightDeltas = wm.computeWeightDeltas(layerError)
            weightAccumulator.getOrPut(wm) {
                Matrix(wm.weightMatrix.nrow(), wm.weightMatrix.ncol())
            }.add(weightDeltas)
            // Note that the source of the weight matrix may be the same across multiple passes through this code,
            // supporting skip connections. In such cases, the backpropagated errors from these connections
            // are added together to form the accumulated error.
            val errors = wm.backpropagateError(layerError)
            accumulatedLayerError = accumulatedLayerError?.add(errors) ?: errors
        }
        // The new error which gets pushed further back until the first layer is reached
        accumulatedLayerError?.let { layerError = it } // null on the first layer
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
            println(wm.weightMatrix)
        }
        println(wm.target)
    }

}
